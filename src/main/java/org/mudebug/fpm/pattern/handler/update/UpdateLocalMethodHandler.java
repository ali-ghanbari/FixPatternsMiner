package org.mudebug.fpm.pattern.handler.update;

import org.mudebug.fpm.pattern.handler.OperationHandler;
import org.mudebug.fpm.pattern.rules.LocalGetterReplacementRule;
import org.mudebug.fpm.pattern.rules.LocalSetterReplacementRule;
import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtVariableReference;

import java.util.List;

public class UpdateLocalMethodHandler extends UpdateHandler {
    public UpdateLocalMethodHandler(OperationHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement e1, CtElement e2) {
        return (e1 instanceof CtInvocation && e2 instanceof CtVariableAccess)
                || (e2 instanceof CtInvocation && e1 instanceof CtVariableAccess);
    }

    private CtInvocation getInvocation(CtElement e1, CtElement e2) {
        return (e1 instanceof CtInvocation) ? (CtInvocation) e1 : (CtInvocation) e2;
    }

    private CtVariableAccess getVarAccess(CtElement e1, CtElement e2) {
        return (e1 instanceof CtVariableAccess) ? (CtVariableAccess) e1 : (CtVariableAccess) e2;
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        final CtVariableAccess va = getVarAccess(e1, e2);
        final CtInvocation in = getInvocation(e1, e2);
        final List<CtExpression> args = in.getArguments();
        final CtVariableReference variable = va.getVariable();
        final CtExecutableReference method = in.getExecutable();
        if (va instanceof CtVariableRead) {
            if (args.size() == 0 && variable.getType().equals(in.getType())) {
                if (va == e1) {
                    return new LocalGetterReplacementRule(variable.getSimpleName(), method.getSignature());
                }
                return new LocalGetterReplacementRule(method.getSignature(), variable.getSimpleName());
            }
        } else if (va instanceof CtVariableWrite) {
            if (args.size() == 1 && args.get(0).getType().equals(variable.getType())) {
                if (va == e1) {
                    return new LocalSetterReplacementRule(variable.getSimpleName(), method.getSignature());
                }
                return new LocalSetterReplacementRule(method.getSignature(), variable.getSimpleName());
            }
        }
        return super.handlePattern(e1, e2);
    }
}
