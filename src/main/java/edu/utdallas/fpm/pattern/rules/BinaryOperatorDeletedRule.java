package edu.utdallas.fpm.pattern.rules;

import spoon.reflect.code.BinaryOperatorKind;

public class BinaryOperatorDeletedRule implements Rule {
    private final BinaryOperatorKind kind;
    private final Operand which;

    public BinaryOperatorDeletedRule(BinaryOperatorKind kind, Operand which) {
        this.kind = kind;
        this.which = which;
    }

    public BinaryOperatorKind getDeletedBinaryOperatorKind() {
        return kind;
    }

    public Operand getDeletedOperand() {
        return which;
    }

    @Override
    public String getId() {
        return String.format("%s (%s of %s)",
                this.getClass().getSimpleName(),
                this.getDeletedOperand().name(),
                this.getDeletedBinaryOperatorKind().name());
    }
}
