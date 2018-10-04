package org.mudebug.fpm.pattern.handler.update;

import org.mudebug.fpm.pattern.rules.*;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtTypeReference;

/**
 * Responsible for method and ctor invocations
 */
public class UpdateInvocationHandler extends UpdateHandler {
    public UpdateInvocationHandler(UpdateHandler next) {
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
        final CtTypeReference recTypeSrc = sin.getTarget().getType();
        final CtTypeReference recTypeDst = din.getTarget().getType();
        if (methodNameDst.equals(methodNameSrc)) {
            if (sin.getArguments().equals(din.getArguments())) {
                if (!recTypeSrc.equals(recTypeDst)) {
                    /* this is a new pattern:
                     *      - method name equal
                     *      - method arguments equal
                     *      - different receiver types*/
                    return new InvRecTypeRule(recTypeSrc, recTypeDst);
                }
            } else {
                if (recTypeSrc.equals(recTypeDst)) {
                    return new ArgumentListRule();
                }
            }
        } else {
            if (sin.getArguments().equals(din.getArguments()) && recTypeSrc.equals(recTypeDst)) {
                return new MethodNameRule(methodNameSrc, methodNameDst);
            }
        }
        return UnknownRule.UNKNOWN_RULE;
    }

    private String getMethodName(final CtInvocation in) {
        return in.getExecutable().getSimpleName();
    }
}
