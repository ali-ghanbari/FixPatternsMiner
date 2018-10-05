package org.mudebug.fpm.pattern.rules;

import spoon.reflect.code.UnaryOperatorKind;

/**
 * a++ -> a--
 * a++ -> --a
 * !a -> !!a
 * a++ -> Math.abs(a) (is not handled)
 * and vice versa
 */
public class UnaryOpReplacementRule implements Rule {
    private final UnaryOperatorKind src;
    private final UnaryOperatorKind dst;

    public UnaryOpReplacementRule(UnaryOperatorKind src, UnaryOperatorKind dst) {
        this.src = src;
        this.dst = dst;
    }
}
