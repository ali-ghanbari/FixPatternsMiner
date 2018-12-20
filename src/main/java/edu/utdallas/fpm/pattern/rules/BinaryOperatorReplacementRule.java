package edu.utdallas.fpm.pattern.rules;

import spoon.reflect.code.BinaryOperatorKind;

public class BinaryOperatorReplacementRule implements Rule {
    private final BinaryOperatorKind src;
    private final BinaryOperatorKind dst;

    public BinaryOperatorReplacementRule(BinaryOperatorKind srcKind, BinaryOperatorKind dstKind) {
        this.src = srcKind;
        this.dst = dstKind;
    }

    public BinaryOperatorKind getSourceBinaryOperatorKind() {
        return src;
    }

    public BinaryOperatorKind getDestinationBinaryOperatorKind() {
        return dst;
    }

    @Override
    public String getId() {
        return String.format("%s (%s -> %s)",
                this.getClass().getSimpleName(),
                this.getSourceBinaryOperatorKind().name(),
                this.getDestinationBinaryOperatorKind().name());
    }
}
