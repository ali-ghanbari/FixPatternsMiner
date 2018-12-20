package edu.utdallas.fpm.pattern.rules;

import edu.utdallas.fpm.pattern.rules.util.SerializableLiteral;
import spoon.reflect.code.CtLiteral;

public class ConstantificationRule implements Rule {
    private final SerializableLiteral literal;

    public ConstantificationRule(final CtLiteral literal) {
        this.literal = SerializableLiteral.fromCtLiteral(literal);
    }

    public SerializableLiteral getLiteral() {
        return literal;
    }

    @Override
    public String getId() {
        final Object value = this.literal.getValue();
        final String strVal;
        if (value == null) {
            strVal = "NULL";
        } else if (value instanceof String) {
            strVal = "SOME_STRING";
        } else {
            strVal = value.toString();
        }
        return String.format("%s (? -> %s)",
                this.getClass().getSimpleName(),
                strVal);
    }
}
