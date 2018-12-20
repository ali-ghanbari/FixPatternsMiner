package edu.utdallas.fpm.pattern.rules;

public enum MemberVarAssignmentDeletionRule implements Rule {
    MEMBER_VAR_ASSIGNMENT_DELETION_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
