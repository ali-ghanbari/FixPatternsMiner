package org.mudebug.fpm.pattern.rules;

import spoon.reflect.code.UnaryOperatorKind;

public class UnaryFunctionToUnaryOperatorRule implements Rule {
    private final String name;
    private final UnaryOperatorKind opKind;

    public UnaryFunctionToUnaryOperatorRule(String name, UnaryOperatorKind opKind) {
        this.name = name;
        this.opKind = opKind;
    }
}
