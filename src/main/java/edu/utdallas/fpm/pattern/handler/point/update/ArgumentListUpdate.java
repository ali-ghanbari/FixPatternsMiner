package edu.utdallas.fpm.pattern.handler.point.update;

import edu.utdallas.fpm.commons.Util;
import edu.utdallas.fpm.pattern.rules.ArgumentListUpdateRule;
import edu.utdallas.fpm.pattern.handler.OperationHandler;
import edu.utdallas.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtElement;

import java.util.Objects;

public class ArgumentListUpdate extends UpdateHandler {
    public ArgumentListUpdate(OperationHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement e1, CtElement e2) {
        return e1 instanceof CtInvocation && e2 instanceof CtInvocation;
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        final CtInvocation sin = (CtInvocation) e1;
        final CtInvocation din = (CtInvocation) e2;
        final String methodNameSrc = getMethodName(sin);
        final String methodNameDst = getMethodName(din);
        if (methodNameDst.equals(methodNameSrc)) {
            if (Objects.equals(sin.getTarget(), din.getTarget())
                    && !Objects.equals(sin.getArguments(), din.getArguments())) {
                if (Util.sibling(sin, din)) {
                    return ArgumentListUpdateRule.ARGUMENT_LIST_UPDATE_RULE;
                }
            }
        }
        return super.handlePattern(e1, e2);
    }

    private String getMethodName(final CtInvocation in) {
        return in.getExecutable().getSimpleName();
    }
}
