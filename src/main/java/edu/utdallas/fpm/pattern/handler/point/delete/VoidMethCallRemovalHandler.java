package edu.utdallas.fpm.pattern.handler.point.delete;

import edu.utdallas.fpm.pattern.handler.OperationHandler;
import edu.utdallas.fpm.pattern.rules.SimpleMethCallRemovalRule;
import edu.utdallas.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtTypeReference;

public class VoidMethCallRemovalHandler extends DeleteHandler {
    protected VoidMethCallRemovalHandler(OperationHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement e1, CtElement e2) {
        return e1 instanceof CtInvocation;
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        final CtInvocation invocation = (CtInvocation) e1;
        final CtTypeReference returnType = invocation.getType();
        if (returnType != null && returnType.toString().equals("void")) {
            return new SimpleMethCallRemovalRule();
        }
        return super.handlePattern(e1, e2);
    }
}
