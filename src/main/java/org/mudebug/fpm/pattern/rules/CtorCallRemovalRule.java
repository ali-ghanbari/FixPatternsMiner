package org.mudebug.fpm.pattern.rules;

import org.mudebug.fpm.pattern.rules.util.SerializableLiteral;
import spoon.reflect.code.CtLiteral;

public class CtorCallRemovalRule implements Rule {
    private final SerializableLiteral literal;

    public CtorCallRemovalRule(CtLiteral literal) {
        this.literal = SerializableLiteral.fromCtLiteral(literal);
    }

    public SerializableLiteral getLiteral() {
        return literal;
    }
}
