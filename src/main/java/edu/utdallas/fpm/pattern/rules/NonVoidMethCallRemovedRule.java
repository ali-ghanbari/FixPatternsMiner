package edu.utdallas.fpm.pattern.rules;

import spoon.reflect.code.CtLiteral;

import static edu.utdallas.fpm.pattern.rules.commons.Util.renderLiteral;

public class NonVoidMethCallRemovedRule implements Rule {
    private final CtLiteral literal;

    public NonVoidMethCallRemovedRule(CtLiteral literal) {
        this.literal = literal;
    }

    public CtLiteral getLiteral() {
        return literal;
    }

    @Override
    public String getId() {
        return String.format("%s (Using %s)",
                this.getClass().getSimpleName(),
                renderLiteral(this.literal));
    }
}
