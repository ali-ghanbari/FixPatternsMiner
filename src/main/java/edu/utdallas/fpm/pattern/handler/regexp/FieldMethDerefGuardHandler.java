package edu.utdallas.fpm.pattern.handler.regexp;

import edu.utdallas.fpm.pattern.handler.util.EitherFieldOrMethod;
import edu.utdallas.fpm.pattern.handler.util.FieldAccess;
import edu.utdallas.fpm.pattern.handler.util.MethodInvocation;
import edu.utdallas.fpm.pattern.rules.DerefGuardRule;
import edu.utdallas.fpm.pattern.rules.MethodGuardRule;
import edu.utdallas.fpm.pattern.rules.UsagePreference;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.MoveOperation;
import gumtree.spoon.diff.operations.Operation;
import edu.utdallas.fpm.pattern.rules.Rule;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtElement;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

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
                    final Pair<UsagePreference, List<EitherFieldOrMethod>> pair =
                            getGuardedDerefs(guardExp, thenExp, elseExp);
                    final List<EitherFieldOrMethod> guardedDerefs = pair.getRight();
                    final UsagePreference usagePreference = pair.getLeft();
                    if (!guardedDerefs.isEmpty()) {
                        return new InsCondState(usagePreference, guardedDerefs);
                    }
                }
            }
            return this;
        }

        private Pair<UsagePreference,
                List<EitherFieldOrMethod>> getGuardedDerefs(final CtExpression guardExp,
                                                            final CtExpression thenExp,
                                                            final CtExpression elseExp) {
            UsagePreference usagePreference = null;
            final List<EitherFieldOrMethod> resList = new ArrayList<>();
            if (guardExp instanceof CtBinaryOperator) {
                final CtBinaryOperator binOp = (CtBinaryOperator) guardExp;
                // checkedExpr == null ? supplant : dereference-of-checkedExpr
                if (binOp.getKind() == BinaryOperatorKind.EQ) {
                    usagePreference = findDerefs(binOp,
                            elseExp.descendantIterator(),
                            thenExp, resList);
                // checkedExpr != null ? dereference-of-checkedExpr : supplant
                } else if (binOp.getKind() == BinaryOperatorKind.NE) {
                    usagePreference = findDerefs(binOp,
                            thenExp.descendantIterator(),
                            elseExp, resList);
                }
            }
            return new ImmutablePair<>(usagePreference, resList);
        }

        private UsagePreference findDerefs(final CtBinaryOperator binOp,
                                           final Iterator<CtElement> dereferencingExpIt,
                                           final CtExpression supplantExp,
                                           final List<EitherFieldOrMethod> resList) {
            final CtExpression lhs = binOp.getLeftHandOperand();
            final CtExpression rhs = binOp.getRightHandOperand();
            if (lhs instanceof CtLiteral ^ rhs instanceof CtLiteral) {
                final CtLiteral literal = (CtLiteral) (lhs instanceof CtLiteral ? lhs : rhs);
                final CtExpression checkedExp = lhs instanceof CtLiteral ? rhs : lhs;
                if (literal.getValue() == null) {
                    final UsagePreference usagePreference =
                            UsagePreference.fromExpression(supplantExp);
                    while (dereferencingExpIt.hasNext()) {
                        final CtElement childElement = dereferencingExpIt.next();
                        if (childElement instanceof CtFieldAccess) {
                            final CtFieldAccess fieldAccess = (CtFieldAccess) childElement;
                            if (Objects.equals(fieldAccess.getTarget(), checkedExp)) {
                                resList.add(new FieldAccess(fieldAccess));
                            }
                        } else if (childElement instanceof CtInvocation) {
                            final CtInvocation invocation = (CtInvocation) childElement;
                            if (Objects.equals(invocation.getTarget(), checkedExp)) {
                                resList.add(new MethodInvocation(invocation));
                            }
                        }
                    }
                    return usagePreference;
                }
            }
            return null;
        }
    }

    private class InsCondState implements State {
        private final UsagePreference usagePreference;
        private final List<EitherFieldOrMethod> guardedAccesses;

        public InsCondState(UsagePreference usagePreference,
                            List<EitherFieldOrMethod> guardedAccesses) {
            this.usagePreference = usagePreference;
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
                    return new IMState(this.usagePreference, eitherFieldOrMethod);
                }
            }
            return initState;
        }
    }

    private class IMState implements AcceptanceState {
        private final UsagePreference usagePreference;
        private final EitherFieldOrMethod eitherFieldOrMethod;

        public IMState(UsagePreference usagePreference,
                       EitherFieldOrMethod eitherFieldOrMethod) {
            this.usagePreference = usagePreference;
            this.eitherFieldOrMethod = eitherFieldOrMethod;
        }

        @Override
        public Rule getRule() {
            if (this.eitherFieldOrMethod instanceof FieldAccess) {
                return new DerefGuardRule(this.usagePreference);
            }
            return new MethodGuardRule(this.usagePreference);
        }

        @Override
        public State handle(Operation operation) {
            throw new UnsupportedOperationException();
        }
    }
}
