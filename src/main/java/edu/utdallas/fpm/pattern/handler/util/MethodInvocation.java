package edu.utdallas.fpm.pattern.handler.util;

import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtInvocation;

public class MethodInvocation implements EitherFieldOrMethod {
    private final CtInvocation invocation;

    public MethodInvocation(CtInvocation invocation) {
        this.invocation = invocation;
    }

    @Override
    public CtFieldAccess getFieldAccess() {
        return null;
    }

    @Override
    public CtInvocation getMethodInvocation() {
        return invocation;
    }
}
