package org.mudebug.fpm.pattern.handler.point.delete;

import org.mudebug.fpm.pattern.handler.OperationHandler;
import org.mudebug.fpm.pattern.rules.Rule;
import org.mudebug.fpm.pattern.rules.SimpleMethCallRemovalRule;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtElement;

public class SimpleMethCallRemovalHandler extends DeleteHandler {
    protected SimpleMethCallRemovalHandler(OperationHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement e1, CtElement e2) {
        return e1 instanceof CtInvocation;
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        return new SimpleMethCallRemovalRule();
    }
}
