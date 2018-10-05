package org.mudebug.fpm.pattern.rules;

public class LocalGetterReplacementRule implements Rule {
    private final String src;
    private final String dst;

    public LocalGetterReplacementRule(String src, String dst) {
        this.src = src;
        this.dst = dst;
    }
}
