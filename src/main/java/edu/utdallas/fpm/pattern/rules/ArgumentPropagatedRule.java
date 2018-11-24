package edu.utdallas.fpm.pattern.rules;

public class ArgumentPropagatedRule implements Rule {
    private final int which;

    public ArgumentPropagatedRule(final int which) {
        this.which = which;
    }

    public int getArgNo() {
        return which;
    }
}
