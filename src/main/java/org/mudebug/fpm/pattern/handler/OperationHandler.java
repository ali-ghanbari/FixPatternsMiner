package org.mudebug.fpm.pattern.handler;

import gumtree.spoon.diff.operations.Operation;
import org.mudebug.fpm.pattern.rules.Rule;
import org.mudebug.fpm.pattern.rules.UnknownRule;
import spoon.reflect.declaration.CtElement;

public abstract class OperationHandler {
    protected OperationHandler next;

    protected OperationHandler(OperationHandler next) {
        this.next = next;
    }

    /*e2 is unused for delete and insert operations*/
    protected abstract boolean canHandlePattern(CtElement e1, CtElement e2);

    /*e2 is unused for delete and insert operations*/
    protected abstract Rule handlePattern(CtElement e1, CtElement e2);

    public abstract boolean canHandleOperation(Operation operation);

    public final Rule handleOperation(final Operation operation) {
        final CtElement src = operation.getSrcNode();
        final CtElement dst = operation.getDstNode();
        return handleOperation(src, dst);
    }

    private Rule handleOperation(final CtElement src, final CtElement dst) {
        final Rule result;
        if (canHandlePattern(src, dst)) {
            result = handlePattern(src, dst);
        } else if (this.next != null) {
            result = this.next.handleOperation(src, dst);
        } else {
            result = new UnknownRule();
        }
        return result;
    }
}