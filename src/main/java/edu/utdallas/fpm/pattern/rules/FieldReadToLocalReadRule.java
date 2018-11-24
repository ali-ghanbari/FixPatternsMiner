package edu.utdallas.fpm.pattern.rules;

public class FieldReadToLocalReadRule implements Rule {
    private final String fieldName;
    private final String localName;

    public FieldReadToLocalReadRule(String fieldName, String localName) {
        this.fieldName = fieldName;
        this.localName = localName;
    }
}
