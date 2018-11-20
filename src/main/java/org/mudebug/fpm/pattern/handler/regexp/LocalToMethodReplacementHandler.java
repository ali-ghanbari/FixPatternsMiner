package org.mudebug.fpm.pattern.handler.regexp;

import gumtree.spoon.diff.operations.*;
import org.mudebug.fpm.pattern.rules.LocalToMethodReplacementRule;
import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtVariableReference;

import java.util.Objects;

import static org.mudebug.fpm.commons.Util.sibling;

public class LocalToMethodReplacementHandler extends RegExpHandler {
    public LocalToMethodReplacementHandler() {
        initState = new InitState();
        this.state = initState;
        this.consumed = 0;
    }

    // we are going to only DI
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
                    return new InsInvState(insertedInvocation);
                }
            } else if (operation instanceof UpdateOperation) {
                final UpdateOperation updOp = (UpdateOperation) operation;
                final CtElement srcElement = updOp.getSrcNode();
                final CtElement dstElement = updOp.getDstNode();
                if (srcElement instanceof CtVariableRead && dstElement instanceof CtVariableRead) {
                    final CtVariableRead srcLocal = (CtVariableRead) srcElement;
                    return new UpdLocalState(srcLocal);
                }
            } else if (operation instanceof DeleteOperation) {
                final DeleteOperation delOp = (DeleteOperation) operation;
                final CtElement deletedElement = delOp.getSrcNode();
                if (deletedElement instanceof CtVariableRead
                        && !(deletedElement instanceof CtFieldAccess)) {
                    final CtVariableRead deletedVarRead = (CtVariableRead) deletedElement;
                    return new DelLocalState(deletedVarRead);
                }
            }
            return initState;
        }
    }

    private class InsInvState implements State {
        private final CtInvocation insertedInvocation;

        public InsInvState(CtInvocation insertedInvocation) {
            this.insertedInvocation = insertedInvocation;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof MoveOperation) {
                final MoveOperation movOp = (MoveOperation) operation;
                final CtElement movedElement = movOp.getSrcNode();
                if (movedElement instanceof CtVariableRead) {
                    final CtVariableReference movedVariable =
                            ((CtVariableRead) movedElement).getVariable();
                    if (Objects.equals(this.insertedInvocation.getType(),
                            movedVariable.getType())) {
                        final String movedLocalName = movedVariable.getSimpleName();
                        final String calleeName = this.insertedInvocation
                                .getExecutable()
                                .getSimpleName();
                        return new U_IMState(movedLocalName, calleeName);
                    }
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
        private final CtVariableRead srcLocal;

        public UpdLocalState(CtVariableRead srcLocal) {
            this.srcLocal = srcLocal;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final InsertOperation insOp = (InsertOperation) operation;
                final CtElement insertedElement = insOp.getSrcNode();
                if (insertedElement instanceof CtInvocation) {
                    final CtInvocation insertedInvocation = (CtInvocation) insertedElement;
                    if (Objects.equals(this.srcLocal.getType(),
                            insertedInvocation.getType())) {
                        if (sibling(this.srcLocal, insertedInvocation)) {
                            return new InsInvState(insertedInvocation);
                        }
                    }
                }
            }
            return initState;
        }
    }

    private class DelLocalState implements State {
        private final CtVariableRead deletedVarRead;

        public DelLocalState(final CtVariableRead deletedVarRead) {
            this.deletedVarRead = deletedVarRead;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final InsertOperation insOp = (InsertOperation) operation;
                final CtElement insertedElement = insOp.getSrcNode();
                if (insertedElement instanceof CtInvocation) {
                    final CtInvocation insertedInvocation =
                            (CtInvocation) insertedElement;
                    if (Objects.equals(insertedInvocation.getType(),
                            this.deletedVarRead.getType())) {
                        if (sibling(this.deletedVarRead, insertedInvocation)) {
                            final String calleeName = insertedInvocation.getExecutable()
                                    .getSimpleName();
                            final String deletedLocalName = deletedVarRead.getVariable()
                                    .getSimpleName();
                            return new DIState(deletedLocalName, calleeName);
                        }
                    }
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
