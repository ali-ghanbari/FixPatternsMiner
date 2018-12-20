package edu.utdallas.fpm.pattern.rules;

public enum LocalToMethodReplacementRule implements Rule {
    LOCAL_TO_METHOD_REPLACEMENT_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
