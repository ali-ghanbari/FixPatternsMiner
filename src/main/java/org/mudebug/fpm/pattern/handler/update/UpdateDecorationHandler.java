package org.mudebug.fpm.pattern.handler.update;

import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.declaration.CtElement;

public class UpdateDecorationHandler extends UpdateHandler {
    public UpdateDecorationHandler(UpdateHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement e1, CtElement e2) {
        return false;
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        return null;
    }
}
