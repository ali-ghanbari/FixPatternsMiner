package org.mudebug.fpm.pattern.rules;

public class LocalWriteToFieldWrite implements Rule {
    private final String deletedLocalName;
    private final String insertedFieldName;

    public LocalWriteToFieldWrite(String deletedLocalName, String insertedFieldName) {
        this.deletedLocalName = deletedLocalName;
        this.insertedFieldName = insertedFieldName;
    }
}
