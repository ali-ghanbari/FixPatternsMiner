package edu.utdallas.fpm.pattern.rules;

import spoon.reflect.code.UnaryOperatorKind;

public class UnaryFunctionToUnaryOperatorRule implements Rule {
    private final String name;
    private final UnaryOperatorKind opKind;

    public UnaryFunctionToUnaryOperatorRule(String name, UnaryOperatorKind opKind) {
        this.name = name;
        this.opKind = opKind;
    }

    @Override
    public String getId() {
        return String.format("%s (%s -> %s)",
                this.getClass().getSimpleName(),
                this.name,
                this.opKind.name());
    }
}
