package org.mudebug.fpm.pattern.handler.regexp;

import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.MoveOperation;
import gumtree.spoon.diff.operations.Operation;
import org.mudebug.fpm.pattern.rules.ArgumentPropagatedRule;
import org.mudebug.fpm.pattern.rules.Rule;
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
                    final CtAbstractInvocation invocation = (CtAbstractInvocation) deletedElement;
                    final List<CtExpression> args = invocation.getArguments();
                    if (invocation instanceof CtInvocation) {
                        final CtInvocation methodInvocation = (CtInvocation) invocation;
                        return new DelState(methodInvocation.getTarget(), args);
                    }
                    return new DelState(args);
                }
            }
            return this;
        }
    }

    private class DelState implements State {
        private final CtExpression rec;
        private final List<CtExpression> args;

        DelState(final List<CtExpression> args) {
            this(null, args);
        }

        DelState(final CtExpression rec, final List<CtExpression> args) {
            this.rec = rec;
            this.args = args;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof MoveOperation) {
                final MoveOperation movOp = (MoveOperation) operation;
                final CtElement movedElement = movOp.getSrcNode();
                if (this.rec != null && this.rec.equals(movedElement)) {
                    return new PropagatedState(0);
                } else {
                    final int which = this.args.indexOf(movedElement);
                    if (which >= 0) {
                        return new PropagatedState(1 + which);
                    }
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
            return new ArgumentPropagatedRule(this.which);
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }
}
