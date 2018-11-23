package org.mudebug.fpm.pattern.rules;

import org.mudebug.fpm.pattern.rules.util.SerializableLiteral;
import spoon.reflect.code.CtLiteral;

public class NonVoidMethCallRemovedRule implements Rule {
    private final SerializableLiteral literal; // whose type equals the deleted method return type

    public NonVoidMethCallRemovedRule(CtLiteral literal) {
        this.literal = SerializableLiteral.fromCtLiteral(literal);
    }

    public SerializableLiteral getLiteral() {
        return literal;
    }
}
