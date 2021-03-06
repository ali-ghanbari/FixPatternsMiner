package edu.utdallas.fpm.pattern.handler.point.update;

import edu.utdallas.fpm.commons.Util;
import edu.utdallas.fpm.pattern.rules.MethodNameReplacementRule;
import edu.utdallas.fpm.pattern.handler.OperationHandler;
import edu.utdallas.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtElement;

import java.util.Objects;

/**
 * Responsible for method invocations only
 */
public class MethodNameReplacement extends UpdateHandler {
    public MethodNameReplacement(OperationHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement src, CtElement dst) {
        return src instanceof CtInvocation && dst instanceof CtInvocation;
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        final CtInvocation sin = (CtInvocation) e1;
        final CtInvocation din = (CtInvocation) e2;
        final String methodNameSrc = getMethodName(sin);
        final String methodNameDst = getMethodName(din);
        if (!methodNameDst.equals(methodNameSrc)) {
            if (Objects.equals(sin.getTarget(), din.getTarget())
                    && Objects.equals(sin.getArguments(), din.getArguments())) {
                if (Util.sibling(sin, din)) {
                    return new MethodNameReplacementRule(methodNameSrc, methodNameDst);
                }
            }
        }
        return super.handlePattern(e1, e2);
    }

    private String getMethodName(final CtInvocation in) {
        return in.getExecutable().getSimpleName();
    }
}
