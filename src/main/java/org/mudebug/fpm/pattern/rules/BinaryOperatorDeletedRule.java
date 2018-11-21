package org.mudebug.fpm.pattern.rules;

import spoon.reflect.code.BinaryOperatorKind;

public class BinaryOperatorDeletedRule implements Rule {
    private final BinaryOperatorKind kind;
    private final Operand which;

    public BinaryOperatorDeletedRule(BinaryOperatorKind kind, Operand which) {
        this.kind = kind;
        this.which = which;
    }

    public BinaryOperatorKind getKind() {
        return kind;
    }

    public Operand getWhich() {
        return which;
    }
}
