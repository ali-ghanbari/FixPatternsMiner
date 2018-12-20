package edu.utdallas.fpm.pattern.handler.regexp;

import edu.utdallas.fpm.pattern.rules.BinaryOperatorDeletedRule;
import edu.utdallas.fpm.pattern.rules.Operand;
import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.MoveOperation;
import gumtree.spoon.diff.operations.Operation;
import edu.utdallas.fpm.pattern.rules.Rule;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtExpression;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtTypeReference;

import java.util.Objects;

import static edu.utdallas.fpm.commons.Util.sibling;

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
                    return new DelState(binOp);
                }
            }
            return this;
        }
    }

    private class DelState implements State {
        private final CtBinaryOperator deletedBinOp;
        private final CtExpression left;
        private final CtExpression right;

        DelState(final CtBinaryOperator deletedBinOp) {
            this.deletedBinOp = deletedBinOp;
            this.left = deletedBinOp.getLeftHandOperand();
            this.right = deletedBinOp.getRightHandOperand();
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof MoveOperation) {
                final MoveOperation movOp = (MoveOperation) operation;
                final CtElement movedElement = movOp.getSrcNode();
                if (movedElement instanceof CtExpression && sibling(movedElement, this.deletedBinOp)) {
                    final CtExpression movedExpr = (CtExpression) movedElement;
                    final CtTypeReference movedExprType = movedExpr.getType();
                    final CtTypeReference deletedBinOpType = this.deletedBinOp.getType();
                    if (Objects.equals(movedExprType, deletedBinOpType)) {
                        final BinaryOperatorKind kind = this.deletedBinOp.getKind();
                        if (Objects.equals(movedExpr, this.left)) {
                            return new PropagatedState(kind, Operand.LEFT);
                        } else if (Objects.equals(movedExpr, this.right)) {
                            return new PropagatedState(kind, Operand.RIGHT);
                        }
                    }
                }
            }
            return initState;
        }
    }

    private class PropagatedState implements AcceptanceState {
        private final BinaryOperatorKind kind;
        private final Operand which;

        PropagatedState(BinaryOperatorKind kind, Operand which) {
            this.kind = kind;
            this.which = which;
        }

        @Override
        public Rule getRule() {
            return new BinaryOperatorDeletedRule(this.kind, this.which);
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }
}
