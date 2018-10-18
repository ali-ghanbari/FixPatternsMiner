package org.mudebug.fpm.pattern.handler.regexp;

import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.MoveOperation;
import gumtree.spoon.diff.operations.Operation;
import org.apache.commons.collections4.iterators.IteratorChain;
import org.mudebug.fpm.pattern.rules.DerefGuardRule;
import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtElement;

import java.util.ArrayList;
import java.util.List;

public class FieldDereferenceGuardHandler extends RegExpHandler {
    public FieldDereferenceGuardHandler() {
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
                    final List<CtFieldAccess> guardedFieldAccesses =
                            getGuardedFieldAccesses(guardExp, thenExp, elseExp);
                    if (!guardedFieldAccesses.isEmpty()) {
                        return new InsCondState(guardedFieldAccesses);
                    }
                }
            }
            return this;
        }

        private List<CtFieldAccess> getGuardedFieldAccesses(final CtExpression guardExp,
                                                           final CtExpression thenExp,
                                                           final CtExpression elseExp) {
            final List<CtFieldAccess> res = new ArrayList<>();
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
                                        res.add(fieldAccess);
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
        private final List<CtFieldAccess> guardedFieldAccesses;

        public InsCondState(List<CtFieldAccess> guardedFieldAccesses) {
            this.guardedFieldAccesses = guardedFieldAccesses;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof MoveOperation) {
                final CtElement movedElement = operation.getSrcNode();
                if (movedElement instanceof CtFieldAccess) {
                    if (this.guardedFieldAccesses.contains(movedElement)) {
                        return new IMState();
                    }
                }
            }
            return initState;
        }
    }

    private class IMState implements AcceptanceState {
        @Override
        public Rule getRule() {
            return new DerefGuardRule();
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }
}
