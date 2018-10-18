package org.mudebug.fpm.pattern.handler.regexp;

import gumtree.spoon.diff.operations.*;
import org.mudebug.fpm.pattern.rules.MethodToLocalReplacementRule;
import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtVariableReference;

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
                    final CtInvocation deletedInvocation = (CtInvocation) deletedElement;
                    final String deletedMethodName = deletedInvocation.getExecutable().getSimpleName();
                    return new UpdLocalDelInvState(deletedMethodName);
                }
            }
            return initState;
        }
    }

    private class UpdLocalDelInvState implements State {
        private final String deletedMethodName;

        public UpdLocalDelInvState(String deletedMethodName) {
            this.deletedMethodName = deletedMethodName;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof MoveOperation) {
                final MoveOperation movOp = (MoveOperation) operation;
                final CtElement movedElement = movOp.getSrcNode();
                if (movedElement instanceof CtVariableRead) {
                    final CtVariableReference movedLocal = ((CtVariableRead) movedElement).getVariable();
                    final String localName = movedLocal.getSimpleName();
                    return new UDMState(this.deletedMethodName, localName);
                }
            }
            return initState;
        }
    }

    private class UDMState implements AcceptanceState {
        private final String methodName;
        private final String localName;

        public UDMState(String methodName, String localName) {
            this.methodName = methodName;
            this.localName = localName;
        }

        @Override
        public Rule getRule() {
            return new MethodToLocalReplacementRule(this.methodName, this.localName);
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }

    private class DelInvState implements State {
        private final CtInvocation deletedInv;
        private final String deletedMethodName;

        public DelInvState(final CtInvocation deletedInv) {
            this.deletedMethodName = deletedInv.getExecutable().getSimpleName();
            this.deletedInv = deletedInv;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final InsertOperation insOp = (InsertOperation) operation;
                final CtElement insertedElement = insOp.getSrcNode();
                // we require that the inserted element and the deleted one
                // be siblings.
                if (insertedElement instanceof CtVariableRead) {
                    final CtVariableReference movedLocal =
                            ((CtVariableRead) insertedElement).getVariable();
                    final String localName = movedLocal.getSimpleName();
                    return new DIState(this.deletedMethodName, localName);
                }
            }
            return initState;
        }
    }

    private class DIState implements AcceptanceState {
        private final String methodName;
        private final String localName;

        public DIState(String methodName, String localName) {
            this.methodName = methodName;
            this.localName = localName;
        }

        @Override
        public Rule getRule() {
            return new MethodToLocalReplacementRule(this.methodName, this.localName);
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }
}
