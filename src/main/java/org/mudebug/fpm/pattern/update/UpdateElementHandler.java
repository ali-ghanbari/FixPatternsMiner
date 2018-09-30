package org.mudebug.fpm.pattern.update;

import org.mudebug.fpm.pattern.rules.Rule;
import org.mudebug.fpm.pattern.UnknownRule;
import spoon.reflect.declaration.CtElement;

public abstract class UpdateElementHandler {
    protected final UpdateElementHandler next;

    public UpdateElementHandler(final UpdateElementHandler next) {
        this.next = next;
    }

    protected abstract boolean canHandle(CtElement src, CtElement dst);

    protected abstract Rule handle(CtElement src, CtElement dst);

    public Rule match(CtElement src, CtElement dst) {
        final Rule result;
        if (canHandle(src, dst)) {
            result = handle(src, dst);
        } else if (this.next != null) {
            result = this.next.match(src, dst);
        } else {
            result = new UnknownRule();
        }
        return result;
    }

    public static UpdateElementHandler createHandlerChain() {
        final UpdateElementHandler chain;
        chain = new UpdateInvocationHandler(null);
        return chain;
    }
}
