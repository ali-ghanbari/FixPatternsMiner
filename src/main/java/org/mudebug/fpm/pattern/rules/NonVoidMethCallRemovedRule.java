package org.mudebug.fpm.pattern.rules;

import spoon.reflect.code.CtLiteral;

public class NonVoidMethCallRemovedRule implements Rule {
    private final CtLiteral literal; // whose type equals the deleted method return type

    public NonVoidMethCallRemovedRule(CtLiteral literal) {
        this.literal = literal;
    }

    public CtLiteral getLiteral() {
        return literal;
    }
}
