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
                        return new DIFieldToMethState();
                    }
                }
            }
            return initState;
        }
    }

    private class DIFieldToMethState implements AcceptanceState {
        @Override
        public Rule getRule() {
            return FieldToMethodReplacementRule.FIELD_TO_METHOD_REPLACEMENT_RULE;
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
                        return new DIMethToFieldState();
                    }
                }
            }
            return initState;
        }
    }

    private class DIMethToFieldState implements AcceptanceState {
        @Override
        public Rule getRule() {
            return MethodToFieldReplacementRule.METHOD_TO_FIELD_REPLACEMENT_RULE;
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }
}
