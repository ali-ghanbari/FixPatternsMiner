package edu.utdallas.fpm.pattern.rules;

import spoon.reflect.code.CtLiteral;

import static edu.utdallas.fpm.pattern.rules.commons.Util.renderLiteral;

/* please note that this pattern is responsible to represent
 * so-called "constantification" meaning turning a non-trivial
 * expression into constant (not local/field read). those cases
 * are supposed to be handled by specialized handlers
 * method -> local and method -> field. */
public class ConstantificationRule implements Rule {
    private final CtLiteral literal;

    public ConstantificationRule(final CtLiteral literal) {
        this.literal = literal;
    }

    public CtLiteral getLiteral() {
        return literal;
    }

    @Override
    public String getId() {
        return String.format("%s (EXPRESSION -> %s)",
                this.getClass().getSimpleName(),
                renderLiteral(this.literal));
    }
}
