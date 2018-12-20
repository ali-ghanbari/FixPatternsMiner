package edu.utdallas.fpm.pattern.rules;

public enum LocalWriteToFieldWriteRule implements Rule {
    LOCAL_WRITE_TO_FIELD_WRITE_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
