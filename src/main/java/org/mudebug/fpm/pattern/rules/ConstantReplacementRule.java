package org.mudebug.fpm.pattern.rules;

public class ConstantReplacementRule implements Rule {
    private final Object src;
    private final Object dst;

    public ConstantReplacementRule(Object src, Object dst) {
        this.src = src;
        this.dst = dst;
    }
}
