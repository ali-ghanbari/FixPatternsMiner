package org.mudebug.fpm.pattern.rules;

/**
 * r1.field1 -> r2.field2
 * where field1 and field2 should be of the same type but have
 * different name. r1 and r2 should be the same
 */
public class FieldNameReplacementRule implements Rule {
    private final String srcFieldName;
    private final String dstFieldName;

    public FieldNameReplacementRule(String srcFieldName, String dstFieldName) {
        this.srcFieldName = srcFieldName;
        this.dstFieldName = dstFieldName;
    }
}
