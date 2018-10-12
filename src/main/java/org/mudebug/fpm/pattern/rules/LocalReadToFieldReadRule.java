package org.mudebug.fpm.pattern.rules;

public class LocalReadToFieldReadRule implements Rule {
    private final String deletedLocalName;
    private final String insertedFieldName;

    public LocalReadToFieldReadRule(String deletedLocalName, String insertedFieldName) {
        this.deletedLocalName = deletedLocalName;
        this.insertedFieldName = insertedFieldName;
    }
}
