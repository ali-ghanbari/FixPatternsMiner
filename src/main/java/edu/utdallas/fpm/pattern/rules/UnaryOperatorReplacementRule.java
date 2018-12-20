package edu.utdallas.fpm.pattern.rules;

import spoon.reflect.code.UnaryOperatorKind;

/**
 * a++ -> a--
 * a++ -> --a
 * !a -> !!a
 * a++ -> Math.abs(a) (is not handled)
 * and vice versa
 */
public class UnaryOperatorReplacementRule implements Rule {
    private final UnaryOperatorKind src;
    private final UnaryOperatorKind dst;

    public UnaryOperatorReplacementRule(UnaryOperatorKind src, UnaryOperatorKind dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    public String getId() {
        return String.format("%s (%s -> %s)",
                this.getClass().getSimpleName(),
                this.src.name(),
                this.dst.name());
    }
}
