package org.mudebug.fpm.pattern.rules;

import spoon.reflect.code.UnaryOperatorKind;

/**
 * a++ -> ++a
 * a-- -> --a
 * NOTE: a++ -> --a is not related to this rule
 */
public class InterFixUnaryOpRepRule implements Rule {
    private final UnaryOperatorKind src;
    private final UnaryOperatorKind dst;

    public InterFixUnaryOpRepRule(UnaryOperatorKind src, UnaryOperatorKind dst) {
        this.src = src;
        this.dst = dst;
    }
}
