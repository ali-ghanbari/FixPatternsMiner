package edu.utdallas.fpm.pattern.rules;

public class FieldToMethodReplacementRule implements Rule {
    private final String deletedFieldName;
    private final String insertedMethodName;

    public FieldToMethodReplacementRule(String deletedFieldName, String insertedMethodName) {
        this.deletedFieldName = deletedFieldName;
        this.insertedMethodName = insertedMethodName;
    }
}
