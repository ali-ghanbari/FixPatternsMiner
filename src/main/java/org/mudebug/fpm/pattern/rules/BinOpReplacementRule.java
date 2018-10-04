package org.mudebug.fpm.pattern.rules;

import spoon.reflect.code.BinaryOperatorKind;

public class BinOpReplacementRule implements Rule {
    private final Object src;
    private final Object dst;

    public BinOpReplacementRule(BinaryOperatorKind srcKind, BinaryOperatorKind dstKind) {
        this.src = srcKind;
        this.dst = dstKind;
    }

    public BinOpReplacementRule(BinaryOperatorKind srcKind, String dstSign) {
        this.src = srcKind;
        this.dst = dstSign;
    }

    public BinOpReplacementRule(String srcSign, BinaryOperatorKind dstKind) {
        this.src = srcSign;
        this.dst = dstKind;
    }
}
