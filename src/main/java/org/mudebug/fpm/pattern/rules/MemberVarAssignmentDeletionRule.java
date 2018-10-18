package org.mudebug.fpm.pattern.rules;

public class MemberVarAssignmentDeletionRule implements Rule {
    private final String fieldName;

    public MemberVarAssignmentDeletionRule(String fieldName) {
        this.fieldName = fieldName;
    }
}
