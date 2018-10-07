package org.mudebug.fpm.pattern.handler.point.update;

import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.Operation;
import gumtree.spoon.diff.operations.UpdateOperation;
import org.mudebug.fpm.pattern.handler.DummyOperationHandler;
import org.mudebug.fpm.pattern.handler.OperationHandler;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtElement;

import java.util.Iterator;

public abstract class UpdateHandler extends OperationHandler {
    protected UpdateHandler(final OperationHandler next) {
        super(next);
    }

    @Override
    public boolean canHandleOperation(final Operation operation) {
        System.out.print(operation.getClass().getName() + " ");
        System.out.println(operation.getSrcNode());
        if (operation instanceof DeleteOperation) {
            final CtElement de = ((DeleteOperation) operation).getSrcNode();
            if (de instanceof CtIf) {
                final CtStatement then = ((CtIf) de).getThenStatement();
                final Iterator<CtStatement> dit = ((CtBlock) then).getStatements().iterator();
                System.out.println("*************");
                while (dit.hasNext()) {
                    final CtElement e = dit.next();
                    System.out.println(e);
                    System.out.println("--");

                }
                System.out.println("*************");
            }
//            final FirstLevelElementIterator dit =
//                    new FirstLevelElementIterator(((DeleteOperation) operation).getSrcNode().asIterable());

        }
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
        return chain;
    }
}
