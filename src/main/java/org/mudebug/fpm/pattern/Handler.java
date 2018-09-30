package org.mudebug.fpm.pattern;

import gumtree.spoon.diff.operations.Operation;
import org.mudebug.fpm.pattern.rules.Rule;

public abstract class Handler {
    protected final Handler next;

    public Handler(final Handler next) {
        this.next = next;
    }

    protected abstract boolean canHandle(Operation operation);

    protected abstract Rule handle(Operation operation);

    public Rule match(Operation operation) {
        final Rule result;
        if (canHandle(operation)) {
            result = handle(operation);
        } else if (this.next != null) {
            result = this.next.match(operation);
        } else {
            result = new UnknownRule();
        }
        return result;
    }

    public static Handler createHandlerChain() {
        final Handler chain;
        chain = new DeleteHandler(new InsertHandler(new UpdateHandler(null)));
        return chain;
    }
}
