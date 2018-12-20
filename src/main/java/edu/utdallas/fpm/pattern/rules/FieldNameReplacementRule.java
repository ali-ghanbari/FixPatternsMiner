package edu.utdallas.fpm.pattern.rules;

public enum FieldNameReplacementRule implements Rule {
    FIELD_NAME_REPLACEMENT_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
