package org.mudebug.fpm.pattern.handler.point.update;

import org.mudebug.fpm.pattern.handler.OperationHandler;
import org.mudebug.fpm.pattern.rules.FieldNameReplacementRule;
import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtFieldReference;

import java.util.Objects;

/**
 * Handles cases like
 *  field1 -> field2
 * where field1 and field2 have the same type but different qualified
 * names. Please note that the restriction over the types is a
 * conservative one, as it does not consider widening and narrowing
 * of the types.
 * We also assume that the receiver expressions are the same.
 */
public class FieldNameReplacement extends UpdateHandler {
    public FieldNameReplacement(OperationHandler next) {
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
        final CtFieldReference f1 = fa1.getVariable();
        final CtFieldReference f2 = fa2.getVariable();
        if (Objects.equals(f1.getType(), f2.getType())) {
            if (Objects.equals(fa1.getTarget(), fa2.getTarget())) {
                final String srcFieldName = f1.getQualifiedName();
                final String dstFieldName = f2.getQualifiedName();
                if (!srcFieldName.equals(dstFieldName)) {
                    return new FieldNameReplacementRule(srcFieldName, dstFieldName);
                }
            }
        }
        return super.handlePattern(e1, e2);
    }
}
