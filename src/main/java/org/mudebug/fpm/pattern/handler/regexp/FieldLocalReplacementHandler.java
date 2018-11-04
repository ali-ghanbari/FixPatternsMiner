package org.mudebug.fpm.pattern.handler.regexp;

import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.Operation;
import org.mudebug.fpm.pattern.rules.*;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtElement;

import static org.mudebug.fpm.commons.Util.sibling;

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
                        final CtFieldWrite fieldWrite = (CtFieldWrite) insertedElement;
                        final String fieldName = fieldWrite.getVariable()
                                .getSimpleName();
                        final String deletedVarName = this.deletedVarWrite
                                .getVariable()
                                .getSimpleName();
                        return new InsFieldWrite(deletedVarName, fieldName);
                    }
                }
            }
            return initState;
        }
    }

    private class InsFieldWrite implements AcceptanceState {
        private final String deletedVarName;
        private final String insertedFieldName;

        public InsFieldWrite(String deletedVarName, String insertedFieldName) {
            this.deletedVarName = deletedVarName;
            this.insertedFieldName = insertedFieldName;
        }

        @Override
        public Rule getRule() {
            return new LocalWriteToFieldWrite(this.deletedVarName, this.insertedFieldName);
        }

        @Override
        public State handle(Operation operation) {
            return initState;
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
                        final CtFieldRead fieldRead = (CtFieldRead) insertedElement;
                        final String fieldName = fieldRead.getVariable()
                                .getSimpleName();
                        final String deletedVarName = this.deletedVarRead
                                .getVariable()
                                .getSimpleName();
                        return new InsFieldRead(deletedVarName, fieldName);
                    }
                }
            }
            return initState;
        }
    }

    private class InsFieldRead implements AcceptanceState {
        private final String deletedVarName;
        private final String insertedFieldName;

        public InsFieldRead(String deletedVarName, String insertedFieldName) {
            this.deletedVarName = deletedVarName;
            this.insertedFieldName = insertedFieldName;
        }

        @Override
        public Rule getRule() {
            return new LocalReadToFieldReadRule(this.deletedVarName, this.insertedFieldName);
        }

        @Override
        public State handle(Operation operation) {
            return initState;
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
                        final CtVariableRead variableRead = (CtVariableRead) insertedElement;
                        final String varName = variableRead.getVariable()
                                .getSimpleName();
                        final String deletedFieldName = this.deletedFieldRead
                                .getVariable()
                                .getSimpleName();
                        return new InsLocalRead(deletedFieldName, varName);
                    }
                }
            }
            return initState;
        }
    }

    private class InsLocalRead implements AcceptanceState {
        private final String deletedFieldName;
        private final String insertedLocalName;

        public InsLocalRead(String deletedFieldName, String insertedLocalName) {
            this.deletedFieldName = deletedFieldName;
            this.insertedLocalName = insertedLocalName;
        }

        @Override
        public Rule getRule() {
            return new FieldReadToLocalReadRule(this.deletedFieldName,
                    this.insertedLocalName);
        }

        @Override
        public State handle(Operation operation) {
            return initState;
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
                        final CtVariableWrite variableWrite =
                                (CtVariableWrite) insertedElement;
                        final String varName = variableWrite.getVariable()
                                .getSimpleName();
                        final String deletedFieldName = this.deletedFieldWrite
                                .getVariable()
                                .getSimpleName();
                        return new InsLocalWrite(deletedFieldName, varName);
                    }
                }
            }
            return initState;
        }
    }

    private class InsLocalWrite implements AcceptanceState {
        private final String deletedFieldName;
        private final String insertedLocalName;

        public InsLocalWrite(String deletedFieldName, String insertedLocalName) {
            this.deletedFieldName = deletedFieldName;
            this.insertedLocalName = insertedLocalName;
        }

        @Override
        public Rule getRule() {
            return new FieldWriteToLocalWrite(deletedFieldName, insertedLocalName);
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }
}
