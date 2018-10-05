package org.mudebug.fpm.pattern.handler.update;

import org.mudebug.fpm.pattern.handler.OperationHandler;
import org.mudebug.fpm.pattern.rules.FieldNameReplacementRule;
import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.declaration.CtElement;

/**
 * Handles cases like
 *  field1 -> field2
 * where field1 and field2 have the same type but different qualified
 * names. Please note that the restriction over the types is a
 * conservative one, as it does not consider widening and narrowing
 * of the types.
 * We also assume that the receiver expressions are the same.
 */
public class UpdateFieldNameHandler extends UpdateHandler {
    public UpdateFieldNameHandler(OperationHandler next) {
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
        if (fa1.getType().equals(fa2.getType())
                && fa1.getTarget().equals(fa2.getTarget())) {
            final String srcFieldName = fa1.getVariable().getQualifiedName();
            final String dstFieldName = fa2.getVariable().getQualifiedName();
            if (!srcFieldName.equals(dstFieldName)) {
                return new FieldNameReplacementRule(srcFieldName, dstFieldName);
            }
        }
        return super.handlePattern(e1, e2);
    }
}
