package edu.utdallas.fpm.pattern.handler.point.update;

import gumtree.spoon.diff.operations.Operation;
import gumtree.spoon.diff.operations.UpdateOperation;
import edu.utdallas.fpm.pattern.handler.DummyOperationHandler;
import edu.utdallas.fpm.pattern.handler.OperationHandler;

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

        chain = new ArgumentListUpdate(chain);
        chain = new CtorReplacement(chain);
        chain = new LocalNameReplacement(chain);
        chain = new ConstantReplacement(chain);
        chain = new FieldNameReplacement(chain);
        chain = new UnaryOperatorReplacement(chain);
        chain = new BinaryOperatorReplacement(chain);
        chain = new MethodNameReplacement(chain);
        chain = new CatchTypeReplacement(chain);
        return chain;
    }
}
