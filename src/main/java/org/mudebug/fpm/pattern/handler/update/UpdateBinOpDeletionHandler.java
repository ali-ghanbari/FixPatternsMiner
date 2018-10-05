package org.mudebug.fpm.pattern.handler.update;

import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.declaration.CtElement;

public class UpdateBinOpDeletionHandler extends UpdateHandler {
    public UpdateBinOpDeletionHandler(UpdateHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement e1, CtElement e2) {
        return (e1 instanceof CtBinaryOperator);
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        return super.handlePattern(e1, e2);
    }
}
