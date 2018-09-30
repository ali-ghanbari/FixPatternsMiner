package org.mudebug.fpm.pattern;

import gumtree.spoon.diff.operations.Operation;
import gumtree.spoon.diff.operations.UpdateOperation;
import org.mudebug.fpm.pattern.rules.Rule;
import org.mudebug.fpm.pattern.update.UpdateElementHandler;
import spoon.reflect.declaration.CtElement;

public class UpdateHandler extends Handler {
    private final UpdateElementHandler handler;

    public UpdateHandler(Handler next) {
        super(next);
        this.handler = UpdateElementHandler.createHandlerChain();
    }

    @Override
    protected boolean canHandle(final Operation operation) {
        return operation instanceof UpdateOperation;
    }

    @Override
    protected Rule handle(Operation operation) {
        final CtElement src = operation.getSrcNode();
        final CtElement dst = operation.getDstNode();
        handler.match(src, dst);
        return null;
    }
}
