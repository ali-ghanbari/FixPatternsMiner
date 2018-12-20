package edu.utdallas.fpm.pattern.rules;

public enum FieldToMethodReplacementRule implements Rule {
    FIELD_TO_METHOD_REPLACEMENT_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
