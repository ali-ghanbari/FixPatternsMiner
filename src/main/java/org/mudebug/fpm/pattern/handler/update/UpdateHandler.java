package org.mudebug.fpm.pattern.handler.update;

import gumtree.spoon.diff.operations.Operation;
import gumtree.spoon.diff.operations.UpdateOperation;
import org.mudebug.fpm.pattern.handler.OperationHandler;

public abstract class UpdateHandler extends OperationHandler {
    protected UpdateHandler(final UpdateHandler next) {
        super(next);
    }

    @Override
    public boolean canHandleOperation(final Operation operation) {
        return operation instanceof UpdateOperation;
    }

    public static UpdateHandler createHandlerChain() {
        final UpdateHandler chain;
        chain = new UpdateInvocationHandler(new UpdateBinOpHandler(new UpdateUnaryOpHandler(null)));
        return chain;
    }
}
