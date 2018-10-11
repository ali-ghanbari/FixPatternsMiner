package org.mudebug.fpm.pattern.rules;

import spoon.reflect.code.BinaryOperatorKind;

public class BiFunctionToBinaryOperatorRule implements Rule {
    private final String name;
    private final BinaryOperatorKind opKind;

    public BiFunctionToBinaryOperatorRule(final String name, BinaryOperatorKind kind) {
        this.name = name;
        this.opKind = kind;
    }
}
