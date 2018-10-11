package org.mudebug.fpm.pattern.handler.regexp;

import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.Operation;
import org.mudebug.fpm.pattern.rules.ConstantificationRule;
import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.CtElement;

public class ConstantificationHandler extends RegExpHandler {
    public ConstantificationHandler() {
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
                if (deletedElement instanceof CtExpression
                        && !(deletedElement instanceof CtLiteral)) {
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
                    return new Replaced(this.expression, insertedLiteral);
                }
            }
            return initState;
        }
    }

    private class Replaced implements AcceptanceState {
        private final CtExpression exp;
        private final CtLiteral lit;

        Replaced(final CtExpression exp, final CtLiteral lit) {
            this.exp = exp;
            this.lit = lit;
        }

        @Override
        public Rule getRule() {
            return new ConstantificationRule(exp.getClass().getName(),
                    String.valueOf(lit.getValue()));
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }
}
