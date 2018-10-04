package org.mudebug.fpm.pattern.handler.update;

import org.mudebug.fpm.pattern.rules.FieldAccessGetterRule;
import org.mudebug.fpm.pattern.rules.Rule;
import org.mudebug.fpm.pattern.rules.UnknownRule;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;

import java.util.List;

public class UpdateFieldMethodHandler extends UpdateHandler {
    protected UpdateFieldMethodHandler(UpdateHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement e1, CtElement e2) {
        return (e1 instanceof CtFieldAccess && e2 instanceof CtInvocation)
                || (e2 instanceof CtFieldAccess && e1 instanceof CtInvocation);
    }

    private CtFieldAccess getFieldAccess(CtElement e1, CtElement e2) {
        return (e1 instanceof CtFieldAccess) ? (CtFieldAccess) e1 : (CtFieldAccess) e2;
    }

    private CtInvocation getInvocation(CtElement e1, CtElement e2) {
        return (e1 instanceof CtInvocation) ? (CtInvocation) e1 : (CtInvocation) e2;
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        final CtFieldAccess fa = getFieldAccess(e1, e2);
        final CtInvocation in = getInvocation(e1, e2);
        final List<CtExpression> args = in.getArguments();
        final CtFieldReference field = fa.getVariable();
        final CtExecutableReference method = in.getExecutable();
        if (fa instanceof CtFieldRead) {
            if (args.size() == 0
                    && field.getType().equals(in.getType())
                    && in.getTarget().equals(fa.getTarget())) {
                if (fa == e1) {
                    return new FieldAccessGetterRule(field.getQualifiedName(), method.getSignature());
                }
                return new FieldAccessGetterRule(method.getSignature(), field.getQualifiedName());
            }
        } else if (fa instanceof CtFieldWrite) {
            if (args.size() == 1
                    && field.getType().equals(args.get(0).getType())
                    && in.getTarget().equals(fa.getTarget())) {
                if (fa == e1) {
                    return new FieldAccessGetterRule(field.getQualifiedName(), method.getSignature());
                }
                return new FieldAccessGetterRule(method.getSignature(), field.getQualifiedName());
            }
        }
        return UnknownRule.UNKNOWN_RULE;
    }
}
