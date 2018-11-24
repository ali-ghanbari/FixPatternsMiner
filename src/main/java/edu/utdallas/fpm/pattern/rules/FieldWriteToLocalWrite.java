package edu.utdallas.fpm.pattern.rules;

public class FieldWriteToLocalWrite implements Rule {
    private final String fieldName;
    private final String localName;

    public FieldWriteToLocalWrite(String fieldName, String localName) {
        this.fieldName = fieldName;
        this.localName = localName;
    }
}
