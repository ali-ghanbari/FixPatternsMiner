package edu.utdallas.fpm.pattern.rules;

public enum MethodNameReplacementRule implements Rule {
    METHOD_NAME_REPLACEMENT_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
