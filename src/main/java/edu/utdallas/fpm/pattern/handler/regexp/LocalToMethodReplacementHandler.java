package edu.utdallas.fpm.pattern.handler.regexp;

import edu.utdallas.fpm.pattern.rules.LocalToMethodReplacementRule;
import gumtree.spoon.diff.operations.*;
import edu.utdallas.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtVariableReference;

import static edu.utdallas.fpm.commons.Util.sibling;

import java.util.Objects;

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
                        return U_IMState.U_IM_STATE;
                    }
                }
            }
            return initState;
        }
    }

    private enum U_IMState implements AcceptanceState {
        U_IM_STATE;

        @Override
        public Rule getRule() {
            return LocalToMethodReplacementRule.LOCAL_TO_METHOD_REPLACEMENT_RULE;
        }

        @Override
        public State handle(Operation operation) {
            throw new UnsupportedOperationException();
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
                            return DIState.DI_STATE;
                        }
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
            return LocalToMethodReplacementRule.LOCAL_TO_METHOD_REPLACEMENT_RULE;
        }

        @Override
        public State handle(Operation operation) {
            throw new UnsupportedOperationException();
        }
    }
}
