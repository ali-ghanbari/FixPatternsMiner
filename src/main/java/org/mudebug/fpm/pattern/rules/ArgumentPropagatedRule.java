package org.mudebug.fpm.pattern.rules;

public class ArgumentPropagatedRule implements Rule {
    private final int which;

    public ArgumentPropagatedRule(final int which) {
        this.which = which;
    }
}
