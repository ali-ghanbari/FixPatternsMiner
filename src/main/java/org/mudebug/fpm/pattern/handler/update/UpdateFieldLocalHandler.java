package org.mudebug.fpm.pattern.handler.update;

import org.mudebug.fpm.pattern.handler.OperationHandler;
import org.mudebug.fpm.pattern.rules.FieldLocalReplacementRule;
import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.declaration.CtElement;

public class UpdateFieldLocalHandler extends UpdateHandler {
    public UpdateFieldLocalHandler(OperationHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement e1, CtElement e2) {
        return (e1 instanceof CtFieldAccess && e2 instanceof CtVariableAccess)
                || (e2 instanceof CtFieldAccess && e1 instanceof CtVariableAccess);
    }

    private CtFieldAccess getFieldAccess(CtElement e1, CtElement e2) {
        return (e1 instanceof CtFieldAccess) ? (CtFieldAccess) e1 : (CtFieldAccess) e2;
    }

    private CtVariableAccess getVarAccess(CtElement e1, CtElement e2) {
        return (e1 instanceof CtVariableAccess) ? (CtVariableAccess) e1 : (CtVariableAccess) e2;
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        final CtFieldAccess fa = getFieldAccess(e1, e2);
        final CtVariableAccess va = getVarAccess(e1, e2);
        if (fa.getType().equals(va.getType())) {
            if (fa == e1) {
                return new FieldLocalReplacementRule(fa.getVariable().getQualifiedName(), va.getVariable().getSimpleName());
            }
            return new FieldLocalReplacementRule(va.getVariable().getSimpleName(), fa.getVariable().getQualifiedName());
        }
        return super.handlePattern(e1, e2);
    }
}
