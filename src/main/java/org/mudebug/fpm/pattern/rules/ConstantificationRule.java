package org.mudebug.fpm.pattern.rules;

import spoon.reflect.code.CtLiteral;

public class ConstantificationRule implements Rule {
    private final CtLiteral literal;

    public ConstantificationRule(final CtLiteral literal) {
        this.literal = literal;
    }
}
