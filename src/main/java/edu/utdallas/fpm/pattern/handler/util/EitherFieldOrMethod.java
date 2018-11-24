package edu.utdallas.fpm.pattern.handler.util;

import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtInvocation;

public interface EitherFieldOrMethod {
    CtFieldAccess getFieldAccess();
    CtInvocation getMethodInvocation();
}