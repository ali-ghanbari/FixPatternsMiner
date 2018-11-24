package edu.utdallas.fpm.pattern.handler.point.delete;

import edu.utdallas.fpm.pattern.handler.DummyOperationHandler;
import edu.utdallas.fpm.pattern.handler.OperationHandler;
import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.Operation;

public abstract class DeleteHandler extends OperationHandler {
    protected DeleteHandler(OperationHandler next) {
        super(next);
    }

    @Override
    public boolean canHandleOperation(final Operation operation) {
        return operation instanceof DeleteOperation;
    }

    public static OperationHandler createHandlerChain() {
        OperationHandler chain;
        chain = DummyOperationHandler.v();

        chain = new CaseRemovalHandler(chain);
        chain = new FieldInitRemovalHandler(chain);
        chain = new VoidMethCallRemovalHandler(chain);
        chain = new IfRemovalHandler(chain);
        return chain;
    }
}
