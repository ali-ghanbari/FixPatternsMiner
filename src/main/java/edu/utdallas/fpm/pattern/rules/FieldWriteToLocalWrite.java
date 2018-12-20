package edu.utdallas.fpm.pattern.rules;

public enum FieldWriteToLocalWrite implements Rule {
    FIELD_WRITE_TO_LOCAL_WRITE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
