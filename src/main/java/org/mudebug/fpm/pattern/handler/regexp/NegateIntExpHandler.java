package org.mudebug.fpm.pattern.handler.regexp;

import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.MoveOperation;
import gumtree.spoon.diff.operations.Operation;
import org.mudebug.fpm.pattern.rules.ConstantReplacementRule;
import org.mudebug.fpm.pattern.rules.NegateIntExpressionRule;
import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.declaration.CtElement;

public class NegateIntExpHandler extends RegExpHandler {
    public NegateIntExpHandler() {
        initState = new InitState();
        this.state = initState;
        this.consumed = 0;
    }

    private class InitState implements State {
        @Override
        public State handle(Operation operation) {
            if (operation instanceof DeleteOperation) {
                final DeleteOperation delOp = (DeleteOperation) operation;
                final CtElement deletedElement = delOp.getSrcNode();
                if (deletedElement instanceof CtExpression) {
                    final CtExpression deletedExpression = (CtExpression) deletedElement;
                    if (deletedExpression instanceof CtUnaryOperator) {
                        final CtUnaryOperator unaryOp =
                                ((CtUnaryOperator) deletedExpression);
                        final UnaryOperatorKind kind = unaryOp.getKind();
                        if (kind == UnaryOperatorKind.NEG) {
                            return new DelNegatedExprState(unaryOp);
                        }
                    }
                    return new DelExpState(deletedExpression);
                }
            } else if (operation instanceof InsertOperation) {
                final InsertOperation insOp = (InsertOperation) operation;
                final CtElement insertedElement = insOp.getSrcNode();
                if (insertedElement instanceof CtUnaryOperator) {
                    final CtUnaryOperator unaryOp = (CtUnaryOperator) insertedElement;
                    if (unaryOp.getKind() == UnaryOperatorKind.NEG) {
                        final CtExpression insertedExpression = unaryOp.getOperand();
                        return new InsNegatedExprState(insertedExpression);
                    }
                }
            }
            return initState;
        }
    }

    private class DelNegatedExprState implements State {
        private final CtExpression deletedOperand;

        public DelNegatedExprState(CtUnaryOperator deletedUnaryOp) {
            this.deletedOperand = deletedUnaryOp.getOperand();
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof MoveOperation) {
                final MoveOperation movOp = (MoveOperation) operation;
                final CtElement movedElement = movOp.getSrcNode();
                if (movedElement instanceof CtExpression) {
                    final CtExpression movedExpression = (CtExpression) movedElement;
                    if (movedExpression.equals(this.deletedOperand)) {
                        return new DMState();
                    }
                }
            } else if (operation instanceof InsertOperation) {
                final InsertOperation insOp = (InsertOperation) operation;
                final CtElement insertedElement = insOp.getSrcNode();
                if (insertedElement instanceof CtExpression) {
                    final CtExpression insertedExpr = (CtExpression) insertedElement;
                    if (insertedExpr.equals(this.deletedOperand)) {
                        if (insertedExpr instanceof CtLiteral) {
                            final CtLiteral insertedLiteral =
                                    ((CtLiteral) insertedExpr);
                            return new ConstantReplacement(insertedLiteral.getValue());
                        }
                        return new DIState();
                    }
                }
            }
            return initState;
        }
    }

    private class DMState implements AcceptanceState {
        @Override
        public Rule getRule() {
            return new NegateIntExpressionRule();
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }

    private class InsNegatedExprState implements State {
        private final CtExpression insertedExpr;

        public InsNegatedExprState(CtExpression insertedExpr) {
            this.insertedExpr = insertedExpr;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof MoveOperation) {
                final MoveOperation movOp = (MoveOperation) operation;
                final CtElement movedElement = movOp.getSrcNode();
                if (movedElement.equals(this.insertedExpr)) {
                    return new IMState();
                }
            }
            return initState;
        }
    }

    private class IMState implements AcceptanceState {
        @Override
        public Rule getRule() {
            return new NegateIntExpressionRule();
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }

    private class DelExpState implements State {
        private final CtExpression deletedExpr;

        public DelExpState(CtExpression deletedExpr) {
            this.deletedExpr = deletedExpr;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final InsertOperation insOp = (InsertOperation) operation;
                final CtElement insertedElement = insOp.getSrcNode();
                if (insertedElement instanceof CtUnaryOperator) {
                    final CtUnaryOperator insertedUnaryOp =
                            (CtUnaryOperator) insertedElement;
                    if (insertedUnaryOp.getKind() == UnaryOperatorKind.NEG) {
                        final CtExpression operand = insertedUnaryOp.getOperand();
                        if (operand.equals(this.deletedExpr)) {
                            if (this.deletedExpr instanceof CtLiteral) {
                                final Object value =
                                        ((CtLiteral) this.deletedExpr).getValue();
                                return new ConstantReplacement(value);
                            }
                            return new DIState();
                        }
                    }
                }
            }
            return initState;
        }
    }

    private class DIState implements AcceptanceState {
        @Override
        public Rule getRule() {
            return new NegateIntExpressionRule();
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }

    private class ConstantReplacement extends DIState {
        private final Object value;

        public ConstantReplacement(Object value) {
            this.value = value;
        }

        @Override
        public Rule getRule() {
            return new ConstantReplacementRule(this.value);
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }
}
