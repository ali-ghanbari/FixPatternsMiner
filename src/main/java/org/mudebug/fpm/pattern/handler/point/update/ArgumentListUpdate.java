package org.mudebug.fpm.pattern.handler.point.update;

import org.mudebug.fpm.commons.Util;
import org.mudebug.fpm.pattern.handler.OperationHandler;
import org.mudebug.fpm.pattern.rules.ArgumentListUpdateRule;
import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtElement;

import java.util.Objects;

import static org.mudebug.fpm.commons.Util.sibling;

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
                if (sibling(sin, din)) {
                    return new ArgumentListUpdateRule();
                }
            }
        }
        return super.handlePattern(e1, e2);
    }

    private String getMethodName(final CtInvocation in) {
        return in.getExecutable().getSimpleName();
    }
}
