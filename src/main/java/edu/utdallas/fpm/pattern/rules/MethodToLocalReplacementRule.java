package edu.utdallas.fpm.pattern.rules;

public enum MethodToLocalReplacementRule implements Rule {
    METHOD_TO_LOCAL_REPLACEMENT_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
