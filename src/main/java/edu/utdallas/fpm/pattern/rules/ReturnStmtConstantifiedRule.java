package edu.utdallas.fpm.pattern.rules;

import edu.utdallas.fpm.pattern.rules.util.SerializableLiteral;
import spoon.reflect.code.CtLiteral;

public class ReturnStmtConstantifiedRule implements Rule {
    private final SerializableLiteral literal;

    public ReturnStmtConstantifiedRule(CtLiteral literal) {
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
        } else if ((value instanceof Integer && (Integer) value == 0)
                || (value instanceof Long && (Long) value == 0)
                || (value instanceof Double && (Double) value == 0)
                || (value instanceof Float && (Float) value == 0)
                || (value instanceof Boolean && !((Boolean) value))
                || (value instanceof Short && (Short) value == 0)
                || (value instanceof Byte && (Byte) value == 0)) {
            strVal = value.toString();
        } else {
            strVal = "SOME " + value.getClass().getSimpleName();
        }
        return String.format("%s (? -> %s)",
                this.getClass().getSimpleName(),
                strVal);
    }
}
