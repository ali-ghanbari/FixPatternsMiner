package org.mudebug.fpm.pattern.handler.util;

import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtInvocation;

public class FieldAccess implements EitherFieldOrMethod {
    private final CtFieldAccess fieldAccess;

    public FieldAccess(CtFieldAccess fieldAccess) {
        this.fieldAccess = fieldAccess;
    }

    @Override
    public CtFieldAccess getFieldAccess() {
        return fieldAccess;
    }

    @Override
    public CtInvocation getMethodInvocation() {
        return null;
    }
}