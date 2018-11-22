package org.mudebug.fpm.pattern.rules;

import spoon.reflect.code.CtLiteral;

public class CtorCallRemovalRule implements Rule {
    private final CtLiteral literal;

    public CtorCallRemovalRule(CtLiteral literal) {
        this.literal = literal;
    }

    public CtLiteral getLiteral() {
        return literal;
    }
}
