package org.mudebug.fpm.pattern.rules;

import spoon.reflect.code.BinaryOperatorKind;

public class BinOpReplacementRule implements Rule {
    private final BinaryOperatorKind srcKind;
    private final BinaryOperatorKind dstKind;

    public BinOpReplacementRule(BinaryOperatorKind srcKind, BinaryOperatorKind dstKind) {
        this.srcKind = srcKind;
        this.dstKind = dstKind;
    }
}
