package org.mudebug.fpm.pattern.rules;

import org.mudebug.fpm.pattern.rules.util.SerializableLiteral;
import spoon.reflect.code.CtLiteral;

public class ReturnStmtConstantifiedRule implements Rule {
    private final SerializableLiteral literal;

    public ReturnStmtConstantifiedRule(CtLiteral literal) {
        this.literal = SerializableLiteral.fromCtLiteral(literal);
    }

    public SerializableLiteral getLiteral() {
        return literal;
    }
}
