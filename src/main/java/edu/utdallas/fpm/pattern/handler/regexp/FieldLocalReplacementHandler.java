package edu.utdallas.fpm.pattern.handler.regexp;

import edu.utdallas.fpm.pattern.rules.*;
import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.Operation;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtElement;

import static edu.utdallas.fpm.commons.Util.sibling;

public class FieldLocalReplacementHandler extends RegExpHandler {
    public FieldLocalReplacementHandler() {
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
                if (deletedElement instanceof CtFieldRead) {
                    final CtFieldRead fieldRead = (CtFieldRead) deletedElement;
                    return new DelFieldRead(fieldRead);
                } else if (deletedElement instanceof CtFieldWrite) {
                    final CtFieldWrite fieldWrite = (CtFieldWrite) deletedElement;
                    return new DelFieldWrite(fieldWrite);
                } else if (deletedElement instanceof CtVariableRead) {
                    final CtVariableRead variableRead = (CtVariableRead) deletedElement;
                    return new DelVarRead(variableRead);
                } else if (deletedElement instanceof CtVariableWrite) {
                    final CtVariableWrite variableWrite = (CtVariableWrite) deletedElement;
                    return new DelVarWrite(variableWrite);
                }
            }
            return initState;
        }
    }

    private class DelVarWrite implements State {
        private final CtVariableWrite deletedVarWrite;

        public DelVarWrite(final CtVariableWrite deletedVarWrite) {
            this.deletedVarWrite = deletedVarWrite;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final InsertOperation insOp = (InsertOperation) operation;
                final CtElement insertedElement = insOp.getSrcNode();
                if (sibling(this.deletedVarWrite, insertedElement)) {
                    if (insertedElement instanceof  CtFieldWrite) {
                        return InsFieldWrite.INS_FIELD_WRITE;
                    }
                }
            }
            return initState;
        }
    }

    private enum InsFieldWrite implements AcceptanceState {
        INS_FIELD_WRITE;

        @Override
        public Rule getRule() {
            return LocalWriteToFieldWriteRule.LOCAL_WRITE_TO_FIELD_WRITE_RULE;
        }

        @Override
        public State handle(Operation operation) {
            throw new UnsupportedOperationException();
        }
    }

    private class DelVarRead implements State {
        private final CtVariableRead deletedVarRead;

        public DelVarRead(final CtVariableRead deletedVarRead) {
            this.deletedVarRead = deletedVarRead;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final InsertOperation insOp = (InsertOperation) operation;
                final CtElement insertedElement = insOp.getSrcNode();
                if (insertedElement instanceof CtFieldRead) {
                    if (sibling(this.deletedVarRead, insertedElement)) {
                        return InsFieldRead.INS_FIELD_READ;
                    }
                }
            }
            return initState;
        }
    }

    private enum InsFieldRead implements AcceptanceState {
        INS_FIELD_READ;

        @Override
        public Rule getRule() {
            return LocalReadToFieldReadRule.LOCAL_READ_TO_FIELD_READ_RULE;
        }

        @Override
        public State handle(Operation operation) {
            throw new UnsupportedOperationException();
        }
    }

    private class DelFieldRead implements State {
        private final CtFieldRead deletedFieldRead;

        public DelFieldRead(final CtFieldRead deletedFieldRead) {
            this.deletedFieldRead = deletedFieldRead;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final InsertOperation insOp = (InsertOperation) operation;
                final CtElement insertedElement = insOp.getSrcNode();
                if (insertedElement instanceof CtVariableRead) {
                    if (sibling(insertedElement, this.deletedFieldRead)) {
                        return InsLocalRead.INS_LOCAL_READ;
                    }
                }
            }
            return initState;
        }
    }

    private enum InsLocalRead implements AcceptanceState {
        INS_LOCAL_READ;

        @Override
        public Rule getRule() {
            return FieldReadToLocalReadRule.FIELD_READ_TO_LOCAL_READ_RULE;
        }

        @Override
        public State handle(Operation operation) {
            throw new UnsupportedOperationException();
        }
    }

    private class DelFieldWrite implements State {
        private final CtFieldWrite deletedFieldWrite;

        public DelFieldWrite(final CtFieldWrite deletedFieldWrite) {
            this.deletedFieldWrite = deletedFieldWrite;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final InsertOperation insOp = (InsertOperation) operation;
                final CtElement insertedElement = insOp.getSrcNode();
                if (insertedElement instanceof CtVariableWrite) {
                    if (sibling(this.deletedFieldWrite, insertedElement)) {
                        return InsLocalWrite.INS_LOCAL_WRITE;
                    }
                }
            }
            return initState;
        }
    }

    private enum InsLocalWrite implements AcceptanceState {
        INS_LOCAL_WRITE;

        @Override
        public Rule getRule() {
            return FieldWriteToLocalWrite.FIELD_WRITE_TO_LOCAL_WRITE;
        }

        @Override
        public State handle(Operation operation) {
            throw new UnsupportedOperationException();
        }
    }
}
