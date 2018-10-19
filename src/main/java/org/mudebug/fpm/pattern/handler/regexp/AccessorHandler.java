package org.mudebug.fpm.pattern.handler.regexp;

import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.Operation;
import org.mudebug.fpm.pattern.rules.FieldToMethodReplacementRule;
import org.mudebug.fpm.pattern.rules.MethodToFieldReplacementRule;
import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtElement;

public class AccessorHandler extends RegExpHandler {
    public AccessorHandler() {
        initState = new InitState();
        this.state = initState;
        this.consumed = 0;
    }

    private class InitState implements State {
        @Override
        public State handle(Operation operation) {
            if (operation instanceof DeleteOperation) {
                final CtElement deletedElement = operation.getSrcNode();
                if (deletedElement instanceof CtInvocation) {
                    final CtInvocation invocation = (CtInvocation) deletedElement;
                    return new DelInvState(invocation.getExecutable().getSignature());
                } else if (deletedElement instanceof CtFieldAccess) {
                    final CtFieldAccess fieldAccess = (CtFieldAccess) deletedElement;
                    return new DelFieldState(fieldAccess.getVariable().getQualifiedName());
                }
            }
            return this;
        }
    }

    private class DelFieldState implements State {
        private final String deletedFieldName;

        public DelFieldState(String deletedFieldName) {
            this.deletedFieldName = deletedFieldName;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final CtElement insertedElement = operation.getSrcNode();
                if (insertedElement instanceof CtInvocation) {
                    final CtInvocation invocation = (CtInvocation) insertedElement;
                    final String methodName = invocation.getExecutable().getSignature();
                    return new DIFieldToMethState(this.deletedFieldName, methodName);
                }
            }
            return initState;
        }
    }

    private class DIFieldToMethState implements AcceptanceState {
        private final String deletedFieldName;
        private final String insertedMethodName;

        public DIFieldToMethState(String deletedFieldName, String insertedMethodName) {
            this.deletedFieldName = deletedFieldName;
            this.insertedMethodName = insertedMethodName;
        }

        @Override
        public Rule getRule() {
            return new FieldToMethodReplacementRule(this.deletedFieldName, this.insertedMethodName);
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }

    private class DelInvState implements State {
        private final String deletedMethodName;

        public DelInvState(String deletedMethodName) {
            this.deletedMethodName = deletedMethodName;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final CtElement insertedElement = operation.getSrcNode();
                if (insertedElement instanceof CtFieldAccess) {
                    final CtFieldAccess fieldAccess = (CtFieldAccess) insertedElement;
                    final String fieldName = fieldAccess.getVariable().getQualifiedName();
                    return new DIMethToFieldState(this.deletedMethodName, fieldName);
                }
            }
            return initState;
        }
    }

    private class DIMethToFieldState implements AcceptanceState {
        private final String deletedMethodName;
        private final String insertedFieldName;

        public DIMethToFieldState(String deletedMethodName, String insertedFieldName) {
            this.deletedMethodName = deletedMethodName;
            this.insertedFieldName = insertedFieldName;
        }

        @Override
        public Rule getRule() {
            return new MethodToFieldReplacementRule(this.deletedMethodName,
                    this.insertedFieldName);
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }
}
