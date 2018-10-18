package org.mudebug.fpm.pattern.rules;

public class DerefGuardRule implements Rule {
    private final String guardedFieldName;

    public DerefGuardRule(String guardedFieldName) {
        this.guardedFieldName = guardedFieldName;
    }
}
