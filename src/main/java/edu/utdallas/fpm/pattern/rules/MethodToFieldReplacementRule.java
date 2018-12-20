package edu.utdallas.fpm.pattern.rules;

public enum MethodToFieldReplacementRule implements Rule {
    METHOD_TO_FIELD_REPLACEMENT_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
