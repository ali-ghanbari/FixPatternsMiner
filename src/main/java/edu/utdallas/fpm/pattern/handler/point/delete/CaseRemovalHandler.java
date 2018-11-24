package edu.utdallas.fpm.pattern.handler.point.delete;

import edu.utdallas.fpm.pattern.rules.CaseRemovalRule;
import edu.utdallas.fpm.pattern.handler.OperationHandler;
import edu.utdallas.fpm.pattern.rules.Rule;
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
