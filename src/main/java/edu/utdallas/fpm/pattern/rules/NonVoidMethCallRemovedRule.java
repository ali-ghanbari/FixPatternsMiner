package edu.utdallas.fpm.pattern.rules;

import edu.utdallas.fpm.pattern.rules.commons.SerializableLiteral;
import spoon.reflect.code.CtLiteral;

import static edu.utdallas.fpm.pattern.rules.commons.Util.isNumeric;
import static edu.utdallas.fpm.pattern.rules.commons.Util.getNumericValue;

public class NonVoidMethCallRemovedRule implements Rule {
    private final SerializableLiteral literal; // whose type equals the deleted method return type

    public NonVoidMethCallRemovedRule(CtLiteral literal) {
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
        } else if ((isNumeric(value) && getNumericValue(value) == 0)
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
