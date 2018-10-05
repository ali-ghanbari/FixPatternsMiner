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
//        System.out.print(operation.getClass().getName() + " ");
//        System.out.println(operation.getSrcNode());
        return operation instanceof UpdateOperation;
    }

    public static OperationHandler createHandlerChain() {
        OperationHandler chain;
        chain = DummyOperationHandler.v();

        chain = new CtorReplacement(chain);
        chain = new LocalNameReplacement(chain);
        chain = new ConstantReplacement(chain);
        chain = new FieldNameReplacement(chain);
        chain = new UnaryOperatorReplacement(chain);
        chain = new BinaryOperatorReplacement(chain);
        chain = new MethodNameReplacement(chain);
        return chain;
    }
}
