package edu.utdallas.fpm.pattern.handler;

import gumtree.spoon.diff.operations.Operation;
import edu.utdallas.fpm.pattern.rules.Rule;
import edu.utdallas.fpm.pattern.rules.UnknownRule;
import spoon.reflect.declaration.CtElement;

public abstract class OperationHandler {
    protected OperationHandler next;

    protected OperationHandler(OperationHandler next) {
        this.next = next;
    }

    /*e2 is unused for delete and insert operations*/
    protected abstract boolean canHandlePattern(CtElement e1, CtElement e2);

    /*e2 is unused for delete and insert operations*/
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        final Rule rule = this.next != null ? this.next.handleOperation(e1, e2) : null;
        return rule;
    }

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
            result = UnknownRule.UNKNOWN_RULE;
        }
        return result;
    }
}
