package org.mudebug.fpm.pattern.handler.point.delete;

import org.mudebug.fpm.pattern.handler.OperationHandler;
import org.mudebug.fpm.pattern.rules.CaseRemovalRule;
import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtCase;
import spoon.reflect.declaration.CtElement;

public class CaseRemovalHandler extends DeleteHandler {
    public CaseRemovalHandler(OperationHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement e1, CtElement e2) {
        return e1 instanceof CtCase;
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        return new CaseRemovalRule();
    }
}
