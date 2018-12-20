package edu.utdallas.fpm.pattern.rules;

public enum LocalReadToFieldReadRule implements Rule {
    LOCAL_READ_TO_FIELD_READ_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
