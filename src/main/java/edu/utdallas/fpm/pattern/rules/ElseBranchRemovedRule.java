package edu.utdallas.fpm.pattern.rules;

public enum ElseBranchRemovedRule implements Rule {
    ELSE_BRANCH_REMOVED_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
