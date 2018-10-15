package org.mudebug.fpm.pattern.rules;

public class ConstantReplacementRule implements Rule {
    private final Object src;
    private final Object dst; // might be null, e.g., in case of 1 => -1 or -1 => 1

    public ConstantReplacementRule(Object val) {
        this(val, null);
    }

    public ConstantReplacementRule(Object src, Object dst) {
        this.src = src;
        this.dst = dst;
    }
}
