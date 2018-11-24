package edu.utdallas.fpm.pattern.handler.point.insert;

import edu.utdallas.fpm.pattern.rules.CaseBreakerBreakInsertionRule;
import edu.utdallas.fpm.pattern.rules.CaseBreakerReturnInsertionRule;
import edu.utdallas.fpm.pattern.handler.OperationHandler;
import edu.utdallas.fpm.pattern.rules.Rule;
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