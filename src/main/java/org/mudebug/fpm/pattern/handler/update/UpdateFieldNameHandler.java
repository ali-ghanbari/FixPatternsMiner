package org.mudebug.fpm.pattern.handler.update;

import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.declaration.CtElement;

/**
 * Handles cases like
 *  field1 -> field2
 * where field1 and field2 have the same type but different signature
 */
public class UpdateFieldNameHandler extends UpdateHandler {
    protected UpdateFieldNameHandler(UpdateHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement e1, CtElement e2) {
        return e1 instanceof CtFieldAccess && e2 instanceof CtFieldAccess;
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        final CtFieldAccess fa1 = (CtFieldAccess) e1;
        final CtFieldAccess fa2 = (CtFieldAccess) e2;
        if (fa1.getType().equals(fa2.getType())) {

        }
        return null;
    }
}
