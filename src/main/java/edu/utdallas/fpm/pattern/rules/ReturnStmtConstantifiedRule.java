package edu.utdallas.fpm.pattern.rules;

import spoon.reflect.code.CtLiteral;

import static edu.utdallas.fpm.pattern.rules.commons.Util.renderLiteral;

public class ReturnStmtConstantifiedRule implements Rule {
    private final CtLiteral literal;

    public ReturnStmtConstantifiedRule(CtLiteral literal) {
        this.literal = literal;
    }

    public CtLiteral getLiteral() {
        return literal;
    }

    @Override
    public String getId() {
        return String.format("%s (Returning %s)",
                this.getClass().getSimpleName(),
                renderLiteral(this.literal));
    }
}
