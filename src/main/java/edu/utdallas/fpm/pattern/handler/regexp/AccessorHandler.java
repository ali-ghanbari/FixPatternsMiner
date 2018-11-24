package edu.utdallas.fpm.pattern.handler.regexp;

import edu.utdallas.fpm.commons.Util;
import edu.utdallas.fpm.pattern.rules.FieldToMethodReplacementRule;
import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.Operation;
import edu.utdallas.fpm.pattern.rules.MethodToFieldReplacementRule;
import edu.utdallas.fpm.pattern.rules.Rule;
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
                    return new DelInvState(invocation);
                } else if (deletedElement instanceof CtFieldAccess) {
                    final CtFieldAccess fieldAccess = (CtFieldAccess) deletedElement;
                    return new DelFieldState(fieldAccess);
                }
            }
            return this;
        }
    }

    private class DelFieldState implements State {
        private final CtFieldAccess deletedFieldAccess;

        public DelFieldState(CtFieldAccess deletedFieldAccess) {
            this.deletedFieldAccess = deletedFieldAccess;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final CtElement insertedElement = operation.getSrcNode();
                if (insertedElement instanceof CtInvocation) {
                    if (Util.sibling(this.deletedFieldAccess, insertedElement)) {
                        final CtInvocation invocation = (CtInvocation) insertedElement;
                        final String methodName = invocation.getExecutable()
                                .getSignature();
                        final String deletedFieldName = this.deletedFieldAccess
                                .getVariable()
                                .getQualifiedName();
                        return new DIFieldToMethState(deletedFieldName, methodName);
                    }
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
            return new FieldToMethodReplacementRule(this.deletedFieldName,
                    this.insertedMethodName);
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }

    private class DelInvState implements State {
        private final CtInvocation deletedMethodInv;

        public DelInvState(CtInvocation deletedMethodInv) {
            this.deletedMethodInv = deletedMethodInv;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final CtElement insertedElement = operation.getSrcNode();
                if (insertedElement instanceof CtFieldAccess) {
                    if (Util.sibling(this.deletedMethodInv, insertedElement)) {
                        final CtFieldAccess fieldAccess = (CtFieldAccess) insertedElement;
                        final String fieldName = fieldAccess.getVariable()
                                .getQualifiedName();
                        final String deletedMethodName = this.deletedMethodInv
                                .getExecutable()
                                .getSignature();
                        return new DIMethToFieldState(deletedMethodName, fieldName);
                    }
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
