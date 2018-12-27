package edu.utdallas.fpm.pattern.rules;

import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtVariableAccess;

import static edu.utdallas.fpm.commons.Util.isDefault;

/* when we return from a method or use an alternative expression in place of
* a field dereference or virtual method call, we have the following options.
* in case of returning the enclosing void method, we have the option VOID, and
* in case we are unable to determine the shape of returned expression, we can
* simply use COMPLEX_EXPR. */
public enum UsagePreference {
    VOID,
    SOME_LOCAL,
    SOME_FIELD,
    DEFAULT_VALUE,
    COMPLEX_EXPR; // non-default constants are also complex

    /* given null VOID will be returned */
    public static UsagePreference fromExpression(final CtExpression expression) {
        if (expression instanceof CtLiteral) {
            final CtLiteral literal = (CtLiteral) expression;
            return isDefault(literal) ? DEFAULT_VALUE : COMPLEX_EXPR;
        } else if (expression instanceof CtVariableAccess) {
            // please note that field access is a special case of
            // variable access.
            if (expression instanceof CtFieldAccess) {
                return SOME_FIELD;
            } else {
                return SOME_LOCAL;
            }
        } else if (expression == null) {
            return VOID;
        } else {
            return COMPLEX_EXPR;
        }
    }


}
