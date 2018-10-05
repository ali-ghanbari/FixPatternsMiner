package org.mudebug.fpm.pattern.handler.update;

import org.mudebug.fpm.pattern.handler.OperationHandler;
import org.mudebug.fpm.pattern.rules.*;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtElement;

/**
 * Responsible for method and ctor invocations
 */
public class UpdateMethodNameHandler extends UpdateHandler {
    public UpdateMethodNameHandler(OperationHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement src, CtElement dst) {
        return src instanceof CtInvocation
                && dst instanceof CtInvocation;
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        final CtInvocation sin = (CtInvocation) e1;
        final CtInvocation din = (CtInvocation) e2;
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
        return super.handlePattern(e1, e2);
    }

    private String getMethodName(final CtInvocation in) {
        return in.getExecutable().getSimpleName();
    }
}
