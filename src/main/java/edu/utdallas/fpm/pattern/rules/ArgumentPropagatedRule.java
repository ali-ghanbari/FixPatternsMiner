package edu.utdallas.fpm.pattern.rules;

public class ArgumentPropagatedRule implements Rule {
    private final int which;

    public ArgumentPropagatedRule(final int which) {
        this.which = which;
    }

    public int getArgNo() {
        return this.which;
    }

    @Override
    public String getId() {
        return String.format("%s (%d)", this.getClass().getSimpleName(), this.which);
    }
}
