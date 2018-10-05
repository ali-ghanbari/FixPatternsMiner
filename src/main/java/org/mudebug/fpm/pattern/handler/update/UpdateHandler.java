package org.mudebug.fpm.pattern.handler.update;

import gumtree.spoon.diff.operations.Operation;
import gumtree.spoon.diff.operations.UpdateOperation;
import org.mudebug.fpm.pattern.handler.DummyOperationHandler;
import org.mudebug.fpm.pattern.handler.OperationHandler;

public abstract class UpdateHandler extends OperationHandler {
    protected UpdateHandler(final OperationHandler next) {
        super(next);
    }

    @Override
    public boolean canHandleOperation(final Operation operation) {
        return operation instanceof UpdateOperation;
    }

    public static OperationHandler createHandlerChain() {
        OperationHandler chain;
        chain = DummyOperationHandler.v();
        chain = new UpdateConstantifyHandler(chain);
        chain = new UpdateNakedMethCallHandler(chain);
        chain = new UpdateConstantHandler(chain);
        chain = new UpdateLocalMethodHandler(chain);
        chain = new UpdateFieldMethodHandler(chain);
        chain = new UpdateFieldNameHandler(chain);
        chain = new UpdateUnaryOpHandler(chain);
        chain = new UpdateBinOpHandler(chain);
        chain = new UpdateMethodNameHandler(chain);
        return chain;
    }
}
