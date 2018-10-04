package org.mudebug.fpm.pattern.rules;

import spoon.reflect.code.UnaryOperatorKind;

/**
 * a++ -> a--
 * a++ -> --a
 * !a -> !!a
 * a++ -> Math.abs(a)
 * and vice versa
 */
public class UnaryOpReplacementRule implements Rule {
    private final Object src;
    private final Object dst;

    public UnaryOpReplacementRule(UnaryOperatorKind src, UnaryOperatorKind dst) {
        this.src = src;
        this.dst = dst;
    }

    public UnaryOpReplacementRule(UnaryOperatorKind srcKind, String dstSign) {
        this.src = srcKind;
        this.dst = dstSign;
    }

    public UnaryOpReplacementRule(String srcSign, UnaryOperatorKind dstKind) {
        this.src = srcSign;
        this.dst = dstKind;
    }
}
