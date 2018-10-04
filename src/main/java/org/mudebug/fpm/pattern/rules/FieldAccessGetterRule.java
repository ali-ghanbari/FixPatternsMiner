package org.mudebug.fpm.pattern.rules;

public class FieldAccessGetterRule implements Rule {
    private final String src;
    private final String dst;

    public FieldAccessGetterRule(String src, String dst) {
        this.src = src;
        this.dst = dst;
    }
}
