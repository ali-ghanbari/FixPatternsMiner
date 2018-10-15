package org.mudebug.fpm.pattern.handler.point.insert;

import org.mudebug.fpm.pattern.handler.OperationHandler;
import org.mudebug.fpm.pattern.rules.CaseBreakerBreakInsertionRule;
import org.mudebug.fpm.pattern.rules.CaseBreakerReturnInsertionRule;
import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtBreak;
import spoon.reflect.code.CtCase;
import spoon.reflect.code.CtReturn;
import spoon.reflect.declaration.CtElement;

public class CaseBreakerHandler extends InsertHandler {
    public CaseBreakerHandler(OperationHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement e1, CtElement e2) {
        return e1 instanceof CtBreak || e1 instanceof CtReturn;
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        final CtElement parent = e1.getParent();
        if (parent instanceof CtCase) {
            if (e1 instanceof CtBreak) {
                return new CaseBreakerBreakInsertionRule();
            }
            return new CaseBreakerReturnInsertionRule();
        }
        return super.handlePattern(e1, e2);
    }
}