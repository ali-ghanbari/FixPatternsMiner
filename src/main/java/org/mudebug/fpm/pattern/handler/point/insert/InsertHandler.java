package org.mudebug.fpm.pattern.handler.point.insert;

import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.Operation;
import org.mudebug.fpm.pattern.handler.DummyOperationHandler;
import org.mudebug.fpm.pattern.handler.OperationHandler;

public abstract class InsertHandler extends OperationHandler {
    protected InsertHandler(OperationHandler next) {
        super(next);
    }

    @Override
    public boolean canHandleOperation(final Operation operation) {
        return operation instanceof InsertOperation;
    }

    public static OperationHandler createHandlerChain() {
        OperationHandler chain;
        chain = DummyOperationHandler.v();

        chain = new CaseBreakerHandler(chain);
        chain = new RetFieldMethDerefGuardHandler(chain);
        chain = new PreconditionAdditionHandler(chain);
        return chain;
    }
}
