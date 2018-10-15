package org.mudebug.fpm.pattern.handler.regexp;

import gumtree.spoon.diff.operations.*;
import org.mudebug.fpm.pattern.rules.LocalToMethodReplacementRule;
import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtVariableReference;

public class LocalToMethodReplacementHandler extends RegExpHandler {
    public LocalToMethodReplacementHandler() {
        initState = new InitState();
        this.state = initState;
        this.consumed = 0;
    }

    // we are going to match either IM, UIM, or DI
    // therefore, this state has 3 outgoing edges:
    //  - Insertion of an invocation
    //  - Update of the local to another local (the
    //    conflict with other patterns will be
    //    resolved later).
    //  - Deletion of the local
    private class InitState implements State {
        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final InsertOperation insOp = (InsertOperation) operation;
                final CtElement insertedElement = insOp.getSrcNode();
                if (insertedElement instanceof CtInvocation) {
                    final CtInvocation insertedInvocation = (CtInvocation) insertedElement;
                    final String calleeName = insertedInvocation.getExecutable().getSimpleName();
                    return new InsInvState(calleeName);
                }
            } else if (operation instanceof UpdateOperation) {
                final UpdateOperation updOp = (UpdateOperation) operation;
                final CtElement srcElement = updOp.getSrcNode();
                final CtElement dstElement = updOp.getDstNode();
                if (srcElement instanceof CtVariableRead && dstElement instanceof CtVariableRead) {
                    return new UpdLocalState();
                }
            } else if (operation instanceof DeleteOperation) {
                final DeleteOperation delOp = (DeleteOperation) operation;
                final CtElement deletedElement = delOp.getSrcNode();
                if (deletedElement instanceof CtVariableRead) {
                    final CtVariableReference deletedLocal = ((CtVariableRead) deletedElement).getVariable();
                    final String deletedLocalName = deletedLocal.getSimpleName();
                    return new DelLocalState(deletedLocalName);
                }
            }
            return initState;
        }
    }

    private class InsInvState implements State {
        private final String calleeName;

        public InsInvState(String calleeName) {
            this.calleeName = calleeName;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof MoveOperation) {
                final MoveOperation movOp = (MoveOperation) operation;
                final CtElement movedElement = movOp.getSrcNode();
                if (movedElement instanceof CtVariableRead) {
                    final CtVariableReference movedVariable = ((CtVariableRead) movedElement).getVariable();
                    final String movedLocalName = movedVariable.getSimpleName();
                    return new U_IMState(movedLocalName, this.calleeName);
                }
            }
            return initState;
        }
    }

    private class U_IMState implements AcceptanceState {
        private final String localName;
        private final String calleeName;

        public U_IMState(String localName, String calleeName) {
            this.localName = localName;
            this.calleeName = calleeName;
        }

        @Override
        public Rule getRule() {
            return new LocalToMethodReplacementRule(this.localName, this.calleeName);
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }

    private class UpdLocalState implements State {
        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final InsertOperation insOp = (InsertOperation) operation;
                final CtElement insertedElement = insOp.getSrcNode();
                if (insertedElement instanceof CtInvocation) {
                    final CtInvocation insertedInvocation = (CtInvocation) insertedElement;
                    final String calleeName = insertedInvocation.getExecutable().getSimpleName();
                    return new InsInvState(calleeName);
                }
            }
            return initState;
        }
    }

    private class DelLocalState implements State {
        private final String deletedLocalName;

        public DelLocalState(String deletedLocalName) {
            this.deletedLocalName = deletedLocalName;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final InsertOperation insOp = (InsertOperation) operation;
                final CtElement insertedElement = insOp.getSrcNode();
                if (insertedElement instanceof CtInvocation) {
                    final CtInvocation insertedInvocation = (CtInvocation) insertedElement;
                    final String calleeName = insertedInvocation.getExecutable().getSimpleName();
                    return new DIState(this.deletedLocalName, calleeName);
                }
            }
            return initState;
        }
    }

    private class DIState implements AcceptanceState {
        private final String localName;
        private final String calleeName;

        public DIState(String localName, String calleeName) {
            this.localName = localName;
            this.calleeName = calleeName;
        }

        @Override
        public Rule getRule() {
            return new LocalToMethodReplacementRule(this.localName, this.calleeName);
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }
}
