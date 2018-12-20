package edu.utdallas.fpm.pattern.rules;

public enum FieldReadToLocalReadRule implements Rule {
    FIELD_READ_TO_LOCAL_READ_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
