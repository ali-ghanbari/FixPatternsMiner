package org.mudebug.fpm.pattern.handler.regexp;

import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.MoveOperation;
import gumtree.spoon.diff.operations.Operation;
import org.apache.commons.collections4.iterators.IteratorChain;
import org.mudebug.fpm.pattern.handler.util.EitherFieldOrMethod;
import org.mudebug.fpm.pattern.handler.util.FieldAccess;
import org.mudebug.fpm.pattern.handler.util.MethodInvocation;
import org.mudebug.fpm.pattern.rules.DerefGuardRule;
import org.mudebug.fpm.pattern.rules.MethodGuardRule;
import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtElement;

import java.util.ArrayList;
import java.util.List;

public class FieldMethDerefGuardHandler extends RegExpHandler {
    public FieldMethDerefGuardHandler() {
        initState = new InitState();
        this.state = initState;
        this.consumed = 0;
    }

    private class InitState implements State {
        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final CtElement insertedElement = operation.getSrcNode();
                if (insertedElement instanceof CtConditional) {
                    final CtConditional insertedCond = (CtConditional) insertedElement;
                    final CtExpression guardExp = insertedCond.getCondition();
                    final CtExpression thenExp = insertedCond.getThenExpression();
                    final CtExpression elseExp = insertedCond.getElseExpression();
                    final List<EitherFieldOrMethod> guardedDerefs =
                            getGuardedDerefs(guardExp, thenExp, elseExp);
                    if (!guardedDerefs.isEmpty()) {
                        return new InsCondState(guardedDerefs);
                    }
                }
            }
            return this;
        }

        private List<EitherFieldOrMethod> getGuardedDerefs(final CtExpression guardExp,
                                                           final CtExpression thenExp,
                                                           final CtExpression elseExp) {
            final List<EitherFieldOrMethod> res = new ArrayList<>();
            if (guardExp instanceof CtBinaryOperator) {
                final CtBinaryOperator binOp = (CtBinaryOperator) guardExp;
                final BinaryOperatorKind binOpKind = binOp.getKind();
                if (binOpKind == BinaryOperatorKind.EQ
                        || binOpKind == BinaryOperatorKind.NE) {
                    final CtExpression lhs = binOp.getLeftHandOperand();
                    final CtExpression rhs = binOp.getRightHandOperand();
                    if (lhs instanceof CtLiteral ^ rhs instanceof CtLiteral) {
                        final CtLiteral literal =
                                (CtLiteral) (lhs instanceof CtLiteral ? lhs : rhs);
                        final CtExpression checkedExp =
                                lhs instanceof CtLiteral ? rhs : lhs;
                        if (literal.getValue() == null) {
                            final IteratorChain<CtElement> it =
                                    new IteratorChain<>(thenExp.descendantIterator(),
                                            elseExp.descendantIterator());
                            while (it.hasNext()) {
                                final CtElement childElement = it.next();
                                if (childElement instanceof CtFieldAccess) {
                                    final CtFieldAccess fieldAccess =
                                            (CtFieldAccess) childElement;
                                    if (fieldAccess.getTarget().equals(checkedExp)) {
                                        res.add(new FieldAccess(fieldAccess));
                                    }
                                } else if (childElement instanceof CtInvocation) {
                                    final CtInvocation invocation =
                                            (CtInvocation) childElement;
                                    if (invocation.getTarget().equals(checkedExp)) {
                                        res.add(new MethodInvocation(invocation));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return res;
        }
    }

    private class InsCondState implements State {
        private final List<EitherFieldOrMethod> guardedAccesses;

        public InsCondState(List<EitherFieldOrMethod> guardedAccesses) {
            this.guardedAccesses = guardedAccesses;
        }

        private FieldAccess get(final CtFieldAccess fieldAccess) {
            for (final EitherFieldOrMethod elem : this.guardedAccesses) {
                if (elem instanceof FieldAccess) {
                    if (elem.getFieldAccess().equals(fieldAccess)) {
                        return (FieldAccess) elem;
                    }
                }
            }
            return null;
        }

        private MethodInvocation get(final CtInvocation invocation) {
            for (final EitherFieldOrMethod elem : this.guardedAccesses) {
                if (elem instanceof MethodInvocation) {
                    if (elem.getMethodInvocation().equals(invocation)) {
                        return (MethodInvocation) elem;
                    }
                }
            }
            return null;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof MoveOperation) {
                final CtElement movedElement = operation.getSrcNode();
                EitherFieldOrMethod eitherFieldOrMethod = null;
                if (movedElement instanceof CtFieldAccess) {
                    eitherFieldOrMethod = get((CtFieldAccess) movedElement);
                } else if (movedElement instanceof CtInvocation) {
                    eitherFieldOrMethod = get((CtInvocation) movedElement);
                }
                if (eitherFieldOrMethod != null) {
                    return new IMState(eitherFieldOrMethod);
                }
            }
            return initState;
        }
    }

    private class IMState implements AcceptanceState {
        private final EitherFieldOrMethod eitherFieldOrMethod;

        public IMState(EitherFieldOrMethod eitherFieldOrMethod) {
            this.eitherFieldOrMethod = eitherFieldOrMethod;
        }

        @Override
        public Rule getRule() {
            if (this.eitherFieldOrMethod instanceof FieldAccess) {
                final CtFieldAccess fieldAccess =
                        this.eitherFieldOrMethod.getFieldAccess();
                return new DerefGuardRule(fieldAccess.getVariable().getQualifiedName());
            }
            final CtInvocation invocation =
                    this.eitherFieldOrMethod.getMethodInvocation();
            return new MethodGuardRule(invocation.getExecutable().getSignature());
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }
}
