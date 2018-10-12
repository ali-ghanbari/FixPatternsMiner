package org.mudebug.fpm.pattern.handler.regexp;

import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.Operation;
import org.mudebug.fpm.pattern.rules.*;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.code.CtVariableWrite;
import spoon.reflect.declaration.CtElement;

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
                    final String fieldName = fieldRead.getVariable().getSimpleName();
                    return new DelFieldRead(fieldName);
                } else if (deletedElement instanceof CtFieldWrite) {
                    final CtFieldWrite fieldWrite = (CtFieldWrite) deletedElement;
                    final String fieldName = fieldWrite.getVariable().getSimpleName();
                    return new DelFieldWrite(fieldName);
                } else if (deletedElement instanceof CtVariableRead) {
                    final CtVariableRead variableRead = (CtVariableRead) deletedElement;
                    final String varName = variableRead.getVariable().getSimpleName();
                    return new DelVarRead(varName);
                } else if (deletedElement instanceof CtVariableWrite) {
                    final CtVariableWrite variableWrite = (CtVariableWrite) deletedElement;
                    final String varName = variableWrite.getVariable().getSimpleName();
                    return new DelVarWrite(varName);
                }
            }
            return initState;
        }
    }

    private class DelVarWrite implements State {
        private final String deletedVarName;

        public DelVarWrite(String deletedVarName) {
            this.deletedVarName = deletedVarName;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final InsertOperation insOp = (InsertOperation) operation;
                final CtElement insertedElement = insOp.getSrcNode();
                if (insertedElement instanceof  CtFieldWrite) {
                    final CtFieldWrite fieldWrite = (CtFieldWrite) insertedElement;
                    final String fieldName = fieldWrite.getVariable().getSimpleName();
                    return new InsFieldWrite(this.deletedVarName, fieldName);
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
        private final String deletedVarName;

        public DelVarRead(String deletedVarName) {
            this.deletedVarName = deletedVarName;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final InsertOperation insOp = (InsertOperation) operation;
                final CtElement insertedElement = insOp.getSrcNode();
                if (insertedElement instanceof  CtFieldRead) {
                    final CtFieldRead fieldRead = (CtFieldRead) insertedElement;
                    final String fieldName = fieldRead.getVariable().getSimpleName();
                    return new InsFieldRead(this.deletedVarName, fieldName);
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
        private final String deletedFieldName;

        public DelFieldRead(String deletedFieldName) {
            this.deletedFieldName = deletedFieldName;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final InsertOperation insOp = (InsertOperation) operation;
                final CtElement insertedElement = insOp.getSrcNode();
                if (insertedElement instanceof CtVariableRead) {
                    final CtVariableRead variableRead = (CtVariableRead) insertedElement;
                    final String varName = variableRead.getVariable().getSimpleName();
                    return new InsLocalRead(this.deletedFieldName, varName);
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
        private final String deletedFieldName;

        public DelFieldWrite(String deletedFieldName) {
            this.deletedFieldName = deletedFieldName;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final InsertOperation insOp = (InsertOperation) operation;
                final CtElement insertedElement = insOp.getSrcNode();
                if (insertedElement instanceof CtVariableWrite) {
                    final CtVariableWrite variableWrite = (CtVariableWrite) insertedElement;
                    final String varName = variableWrite.getVariable().getSimpleName();
                    return new InsLocalWrite(this.deletedFieldName, varName);
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
