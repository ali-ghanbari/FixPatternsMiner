package org.mudebug.fpm.pattern.rules;

import spoon.reflect.code.CtLiteral;

public class ReturnStmtConstantifiedRule implements Rule {
    private final CtLiteral literal;

    public ReturnStmtConstantifiedRule(CtLiteral literal) {
        this.literal = literal;
    }

    public CtLiteral getLiteral() {
        return literal;
    }
}
