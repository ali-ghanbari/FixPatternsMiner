package org.mudebug.fpm.pattern.handler.point.insert;

import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.Operation;
import org.mudebug.fpm.pattern.handler.OperationHandler;


public abstract class InsertHandler extends OperationHandler {
    protected InsertHandler(InsertHandler next) {
        super(next);
    }

    @Override
    public boolean canHandleOperation(final Operation operation) {
        return operation instanceof InsertOperation;
    }

    public static InsertHandler createHandlerChain() {
        return null;
    }
}
