package edu.utdallas.fpm.pattern.rules;

import spoon.reflect.code.UnaryOperatorKind;

public class UnaryFunctionToUnaryOperatorRule implements Rule {
    private final String name;
    private final UnaryOperatorKind opKind;

    public UnaryFunctionToUnaryOperatorRule(String name, UnaryOperatorKind opKind) {
        this.name = name;
        this.opKind = opKind;
    }

    public String getFunctionName() {
        return name;
    }

    public UnaryOperatorKind getDestinationOperatorKind() {
        return opKind;
    }

    @Override
    public String getId() {
        return String.format("%s (%s -> %s)",
                this.getClass().getSimpleName(),
                this.getFunctionName(),
                this.getDestinationOperatorKind().name());
    }
}
