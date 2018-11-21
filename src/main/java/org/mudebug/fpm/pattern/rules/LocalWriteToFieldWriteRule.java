package org.mudebug.fpm.pattern.rules;

public class LocalWriteToFieldWriteRule implements Rule {
    private final String deletedLocalName;
    private final String insertedFieldName;

    public LocalWriteToFieldWriteRule(String deletedLocalName, String insertedFieldName) {
        this.deletedLocalName = deletedLocalName;
        this.insertedFieldName = insertedFieldName;
    }
}
