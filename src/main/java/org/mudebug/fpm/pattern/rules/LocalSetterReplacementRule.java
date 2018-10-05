package org.mudebug.fpm.pattern.rules;

public class LocalSetterReplacementRule implements Rule {
    private final String src;
    private final String dst;

    public LocalSetterReplacementRule(String src, String dst) {
        this.src = src;
        this.dst = dst;
    }
}
