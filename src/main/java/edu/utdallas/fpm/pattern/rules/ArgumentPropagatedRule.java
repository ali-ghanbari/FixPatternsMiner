package edu.utdallas.fpm.pattern.rules;

public enum ArgumentPropagatedRule implements Rule {
    ARGUMENT_PROPAGATED_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
