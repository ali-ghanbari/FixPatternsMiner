package org.mudebug.fpm.pattern.handler.regexp;

import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.Operation;
import org.mudebug.fpm.pattern.rules.ConstantificationRule;
import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.declaration.CtElement;

public class ConstantificationHandler extends RegExpHandler {
    public ConstantificationHandler() {
        initState = new InitState();
        this.state = initState;
        this.consumed = 0;
    }

    private class InitState implements State {
        private boolean isTrivialExp(final CtExpression exp) {
            if (exp instanceof CtUnaryOperator) {
                final CtUnaryOperator unaryOp = (CtUnaryOperator) exp;
                final UnaryOperatorKind kind = unaryOp.getKind();
                if (kind == UnaryOperatorKind.NEG || kind == UnaryOperatorKind.POS) {
                    final CtExpression operand = unaryOp.getOperand();
                    return operand instanceof CtLiteral;
                }
                return false;
            }
            return exp instanceof CtLiteral;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof DeleteOperation) {
                final DeleteOperation delOp = (DeleteOperation) operation;
                final CtElement deletedElement = delOp.getSrcNode();
                if (deletedElement instanceof CtExpression
                        && !isTrivialExp((CtExpression) deletedElement)) {
                    final CtExpression expression = (CtExpression) deletedElement;
                    return new DelState(expression);
                }
            }
            return initState;
        }
    }

    private class DelState implements State {
        private final CtExpression expression;

        DelState(final CtExpression expression) {
            this.expression = expression;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final InsertOperation insOp = (InsertOperation) operation;
                final CtElement insertedElement = insOp.getSrcNode();
                if (insertedElement instanceof CtLiteral) {
                    final CtLiteral insertedLiteral = (CtLiteral) insertedElement;
                    return new Replaced(this.expression, false, insertedLiteral);
                } else if (insertedElement instanceof CtUnaryOperator) {
                    final CtUnaryOperator unaryOperator = (CtUnaryOperator) insertedElement;
                    final UnaryOperatorKind kind = unaryOperator.getKind();
                    if (kind == UnaryOperatorKind.NEG || kind == UnaryOperatorKind.POS) {
                        final CtExpression operand = unaryOperator.getOperand();
                        if (operand instanceof CtLiteral) {
                            final CtLiteral insertedLiteral = (CtLiteral) operand;
                            return new Replaced(this.expression,
                                    (kind == UnaryOperatorKind.NEG),
                                    insertedLiteral);
                        }
                    }
                }
            }
            return initState;
        }
    }

    private class Replaced implements AcceptanceState {
        private final CtExpression exp;
        private final CtLiteral lit;
        private final boolean negate;

        Replaced(final CtExpression exp, boolean negate, final CtLiteral lit) {
            this.exp = exp;
            this.lit = lit;
            this.negate = negate;
        }

        @Override
        public Rule getRule() {
            return new ConstantificationRule(exp.getClass().getName(),
                    (this.negate ? "-" : "") + String.valueOf(lit.getValue()));
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }
}
