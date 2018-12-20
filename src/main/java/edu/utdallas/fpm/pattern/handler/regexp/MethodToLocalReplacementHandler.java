package edu.utdallas.fpm.pattern.handler.regexp;

import edu.utdallas.fpm.pattern.rules.MethodToLocalReplacementRule;
import gumtree.spoon.diff.operations.*;
import edu.utdallas.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtElement;

import static edu.utdallas.fpm.commons.Util.sibling;

public class MethodToLocalReplacementHandler extends RegExpHandler {
    public MethodToLocalReplacementHandler() {
        initState = new InitState();
        this.state = initState;
        this.consumed = 0;
    }

    // we are going to match either UDM or DI
    // therefore, this state has 2 outgoing edges:
    //  - Update: the receiver of the invocation
    //    is a local, and it is updated to the
    //    destination local
    //  - Deletion of the invocation
    private class InitState implements State {
        @Override
        public State handle(Operation operation) {
            if (operation instanceof UpdateOperation) {
                final UpdateOperation updOp = (UpdateOperation) operation;
                final CtElement srcElement = updOp.getSrcNode();
                final CtElement dstElement = updOp.getDstNode();
                if (srcElement instanceof CtVariableRead && dstElement instanceof CtVariableRead) {
                    return new UpdLocalState();
                }
            } else if (operation instanceof DeleteOperation) {
                final DeleteOperation delOp = (DeleteOperation) operation;
                final CtElement deletedElement = delOp.getSrcNode();
                if (deletedElement instanceof CtInvocation) {
                    final CtInvocation deletedInvocation = (CtInvocation) deletedElement;
                    return new DelInvState(deletedInvocation);
                }
            }
            return initState;
        }
    }

    private class UpdLocalState implements State {
        @Override
        public State handle(Operation operation) {
            if (operation instanceof DeleteOperation) {
                final DeleteOperation delOp = (DeleteOperation) operation;
                final CtElement deletedElement = delOp.getSrcNode();
                if (deletedElement instanceof CtInvocation) {
                    return new UpdLocalDelInvState();
                }
            }
            return initState;
        }
    }

    private class UpdLocalDelInvState implements State {
        @Override
        public State handle(Operation operation) {
            if (operation instanceof MoveOperation) {
                final MoveOperation movOp = (MoveOperation) operation;
                final CtElement movedElement = movOp.getSrcNode();
                if (movedElement instanceof CtVariableRead
                        && !(movedElement instanceof CtFieldAccess)) {
                    return UDMState.UDM_STATE;
                }
            }
            return initState;
        }
    }

    private enum UDMState implements AcceptanceState {
        UDM_STATE;

        @Override
        public Rule getRule() {
            return MethodToLocalReplacementRule.METHOD_TO_LOCAL_REPLACEMENT_RULE;
        }

        @Override
        public State handle(Operation operation) {
            throw new UnsupportedOperationException();
        }
    }

    private class DelInvState implements State {
        private final CtInvocation deletedInv;

        public DelInvState(final CtInvocation deletedInv) {
            this.deletedInv = deletedInv;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final InsertOperation insOp = (InsertOperation) operation;
                final CtElement insertedElement = insOp.getSrcNode();
                if (insertedElement instanceof CtVariableRead
                        && !(insertedElement instanceof CtFieldAccess)) {
                    if (sibling(insertedElement, this.deletedInv)) {
                        return DIState.DI_STATE;
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
            return MethodToLocalReplacementRule.METHOD_TO_LOCAL_REPLACEMENT_RULE;
        }

        @Override
        public State handle(Operation operation) {
            throw new UnsupportedOperationException();
        }
    }
}
