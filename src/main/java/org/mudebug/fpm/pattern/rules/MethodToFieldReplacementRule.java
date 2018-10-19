package org.mudebug.fpm.pattern.rules;

public class MethodToFieldReplacementRule implements Rule {
    private final String deletedMethodName;
    private final String insertedFieldName;

    public MethodToFieldReplacementRule(String deletedMethodName, String insertedFieldName) {
        this.deletedMethodName = deletedMethodName;
        this.insertedFieldName = insertedFieldName;
    }
}
