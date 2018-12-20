package edu.utdallas.fpm.pattern.rules;

import edu.utdallas.fpm.pattern.rules.commons.SerializableLiteral;
import edu.utdallas.fpm.pattern.rules.commons.Util;
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
        final Object value = this.getLiteral().getValue();
        final String strVal;
        if (value == null) {
            strVal = "NULL";
        } else if ((Util.isNumeric(value) && Util.getNumericValue(value) == 0D)
                || (value instanceof Boolean && !((Boolean) value))) {
            strVal = value.toString();
        } else {
            strVal = "SOME " + value.getClass().getSimpleName();
        }
        return String.format("%s (? -> %s)",
                this.getClass().getSimpleName(),
                strVal);
    }
}
