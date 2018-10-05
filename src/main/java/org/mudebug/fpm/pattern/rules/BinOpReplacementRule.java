package org.mudebug.fpm.pattern.rules;

import spoon.reflect.code.BinaryOperatorKind;

public class BinOpReplacementRule implements Rule {
    private final BinaryOperatorKind src;
    private final BinaryOperatorKind dst;

    public BinOpReplacementRule(BinaryOperatorKind srcKind, BinaryOperatorKind dstKind) {
        this.src = srcKind;
        this.dst = dstKind;
    }
}
