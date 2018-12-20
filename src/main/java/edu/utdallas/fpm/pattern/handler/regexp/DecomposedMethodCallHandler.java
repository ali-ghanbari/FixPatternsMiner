package edu.utdallas.fpm.pattern.handler.regexp;

import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.MoveOperation;
import gumtree.spoon.diff.operations.Operation;
import edu.utdallas.fpm.pattern.rules.ArgumentPropagatedRule;
import edu.utdallas.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtAbstractInvocation;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtElement;

import java.util.List;

public class DecomposedMethodCallHandler extends RegExpHandler {
    public DecomposedMethodCallHandler() {
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
                if (deletedElement instanceof CtAbstractInvocation) {
                    final CtAbstractInvocation invocation =
                            (CtAbstractInvocation) deletedElement;
                    return new DelState(invocation);
                }
            }
            return this;
        }
    }

    private class DelState implements State {
        private final CtExpression rec;
        private final List<CtExpression> args;

        DelState(final CtAbstractInvocation deletedInv) {
            this.args = deletedInv.getArguments();
            if (deletedInv instanceof CtInvocation) {
                final CtInvocation methodInvocation = (CtInvocation) deletedInv;
                this.rec = methodInvocation.getTarget();
            } else {
                this.rec = null;
            }
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof MoveOperation) {
                final MoveOperation movOp = (MoveOperation) operation;
                final CtElement movedElement = movOp.getDstNode();
                if (this.rec != null && this.rec.equals(movedElement)) {
                    return new PropagatedState();
                } else {
                    final int which = this.args.indexOf(movedElement);
                    if (which >= 0) {
                        return new PropagatedState();
                    }
                }
            }
            return initState;
        }
    }

    private class PropagatedState implements AcceptanceState {
        @Override
        public Rule getRule() {
            return ArgumentPropagatedRule.ARGUMENT_PROPAGATED_RULE;
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }
}
