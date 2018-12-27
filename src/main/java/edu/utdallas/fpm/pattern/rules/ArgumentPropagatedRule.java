package edu.utdallas.fpm.pattern.rules;

/* this pattern subsumes both argument propagation and naked receiver mutators */
public enum ArgumentPropagatedRule implements Rule {
    ARGUMENT_PROPAGATED_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
