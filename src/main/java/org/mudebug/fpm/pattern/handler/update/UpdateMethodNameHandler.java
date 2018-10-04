package org.mudebug.fpm.pattern.handler.update;

import org.mudebug.fpm.pattern.rules.*;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtTypeReference;

/**
 * Responsible for method and ctor invocations
 */
public class UpdateMethodNameHandler extends UpdateHandler {
    public UpdateMethodNameHandler(UpdateHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement src, CtElement dst) {
        return src instanceof CtInvocation
                && dst instanceof CtInvocation;
    }

    @Override
    protected Rule handlePattern(CtElement src, CtElement dst) {
        final CtInvocation sin = (CtInvocation) src;
        final CtInvocation din = (CtInvocation) dst;
        final String methodNameSrc = getMethodName(sin);
        final String methodNameDst = getMethodName(din);
        if (sin.getTarget().equals(din.getTarget())) {
            if (methodNameDst.equals(methodNameSrc)) {
                if (!sin.getArguments().equals(din.getArguments())) {
                    return new ArgumentListRule();
                }
            } else {
                if (sin.getArguments().equals(din.getArguments())) {
                    return new MethodNameRule(methodNameSrc, methodNameDst);
                }
            }
        }
        return UnknownRule.UNKNOWN_RULE;
    }

    private String getMethodName(final CtInvocation in) {
        return in.getExecutable().getSimpleName();
    }
}
