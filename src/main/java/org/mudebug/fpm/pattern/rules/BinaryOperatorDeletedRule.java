package org.mudebug.fpm.pattern.rules;

public class BinaryOperatorDeletedRule implements Rule {
    private final int which;

    public BinaryOperatorDeletedRule(int which) {
        this.which = which;
    }
}
