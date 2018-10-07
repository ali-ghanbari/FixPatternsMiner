package org.mudebug.fpm.pattern.handler.point.delete;

import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.Operation;
import org.mudebug.fpm.pattern.handler.OperationHandler;

public abstract class DeleteHandler extends OperationHandler {
    protected DeleteHandler(DeleteHandler next) {
        super(next);
    }

    @Override
    public boolean canHandleOperation(final Operation operation) {
        return operation instanceof DeleteOperation;
    }

    public static DeleteHandler createHandlerChain() {
        return null;
    }
}
