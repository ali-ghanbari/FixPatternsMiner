package edu.utdallas.fpm.pattern.rules;

import spoon.reflect.code.BinaryOperatorKind;

public class BiFunctionToBinaryOperatorRule implements Rule {
    private final String name;
    private final BinaryOperatorKind opKind;

    public BiFunctionToBinaryOperatorRule(final String name, BinaryOperatorKind kind) {
        this.name = name;
        this.opKind = kind;
    }

    public String getBiFunctionName() {
        return this.name;
    }

    public BinaryOperatorKind getBinaryOperatorKind() {
        return this.opKind;
    }

    @Override
    public String getId() {
        return String.format("%s (%s -> %s)",
                this.getClass().getSimpleName(),
                this.getBiFunctionName(),
                this.getBinaryOperatorKind().name());
    }
}
