package org.mudebug.fpm.pattern.rules;

public class FieldAccessSetterRule implements Rule {
    private final String src;
    private final String dst;

    public FieldAccessSetterRule(String src, String dst) {
        this.src = src;
        this.dst = dst;
    }
}
