package org.mudebug.fpm.pattern.handler.regexp;

import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.MoveOperation;
import gumtree.spoon.diff.operations.Operation;
import org.mudebug.fpm.pattern.rules.BinaryOperatorDeletedRule;
import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtExpression;
import spoon.reflect.declaration.CtElement;

public class DecomposeBinaryOperatorHandler extends RegExpHandler {
    public DecomposeBinaryOperatorHandler() {
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
                if (deletedElement instanceof CtBinaryOperator) {
                    final CtBinaryOperator binOp = (CtBinaryOperator) deletedElement;
                    final CtExpression left = binOp.getLeftHandOperand();
                    final CtExpression right = binOp.getRightHandOperand();
                    return new DelState(left, right);
                }
            }
            return this;
        }
    }

    private class DelState implements State {
        private final CtExpression left;
        private final CtExpression right;

        DelState(final CtExpression left, final CtExpression right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof MoveOperation) {
                final MoveOperation movOp = (MoveOperation) operation;
                final CtElement movedElement = movOp.getSrcNode();
                if (this.left.equals(movedElement)) {
                    return new PropagatedState(0);
                } else if (this.right.equals(movedElement)) {
                    return new PropagatedState(1);
                }
            }
            return initState;
        }
    }

    private class PropagatedState implements AcceptanceState {
        private final int which;

        PropagatedState(int which) {
            this.which = which;
        }

        @Override
        public Rule getRule() {
            return new BinaryOperatorDeletedRule(this.which);
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }
}
