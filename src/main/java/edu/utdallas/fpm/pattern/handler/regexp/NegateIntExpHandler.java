package edu.utdallas.fpm.pattern.handler.regexp;

import edu.utdallas.fpm.pattern.rules.ConstantReplacementRule;
import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.MoveOperation;
import gumtree.spoon.diff.operations.Operation;
import edu.utdallas.fpm.pattern.rules.NegateIntExpressionRule;
import edu.utdallas.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.declaration.CtElement;

import java.util.Objects;

import static edu.utdallas.fpm.commons.Util.textEquals;

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
                    if (Objects.equals(movedExpression, this.deletedOperand)) {
                        return DMState.DM_STATE;
                    }
                }
            } else if (operation instanceof InsertOperation) {
                final InsertOperation insOp = (InsertOperation) operation;
                final CtElement insertedElement = insOp.getSrcNode();
                if (insertedElement instanceof CtExpression) {
                    final CtExpression insertedExpr = (CtExpression) insertedElement;
                    if (textEquals(insertedExpr, this.deletedOperand)) {
                        if (insertedExpr instanceof CtLiteral) {
                            final CtLiteral insertedLiteral =
                                    ((CtLiteral) insertedExpr);
                            return new ConstantReplacement(insertedLiteral);
                        }
                        return DIState.DI_STATE;
                    }
                }
            }
            return initState;
        }
    }

    private enum DMState implements AcceptanceState {
        DM_STATE;

        @Override
        public Rule getRule() {
            return NegateIntExpressionRule.NEGATE_INT_EXPRESSION_RULE;
        }

        @Override
        public State handle(Operation operation) {
            throw new UnsupportedOperationException();
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
                if (Objects.equals(movedElement, this.insertedExpr)) {
                    return IMState.IM_STATE;
                }
            }
            return initState;
        }
    }

    private enum IMState implements AcceptanceState {
        IM_STATE;

        @Override
        public Rule getRule() {
            return NegateIntExpressionRule.NEGATE_INT_EXPRESSION_RULE;
        }

        @Override
        public State handle(Operation operation) {
            throw new UnsupportedOperationException();
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
                        if (textEquals(operand, this.deletedExpr)) {
                            if (this.deletedExpr instanceof CtLiteral) {
                                final CtLiteral deletedLiteral = (CtLiteral) this.deletedExpr;
                                return new ConstantReplacement(deletedLiteral);
                            }
                            return DIState.DI_STATE;
                        }
                    }
                }
            }
            return initState;
        }
    }

    private enum DIState implements AcceptanceState {
        DI_STATE;

        @Override
        public Rule getRule() {
            return NegateIntExpressionRule.NEGATE_INT_EXPRESSION_RULE;
        }

        @Override
        public State handle(Operation operation) {
            throw new UnsupportedOperationException();
        }
    }

    private class ConstantReplacement implements AcceptanceState {
        private final CtLiteral literal;

        public ConstantReplacement(CtLiteral literal) {
            this.literal = literal;
        }

        @Override
        public Rule getRule() {
            return new ConstantReplacementRule(this.literal);
        }

        @Override
        public State handle(Operation operation) {
            throw new UnsupportedOperationException();
        }
    }
}
