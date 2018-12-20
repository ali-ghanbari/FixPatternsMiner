package edu.utdallas.fpm.pattern.rules;

public enum ThenBranchRemovedRule implements Rule {
    THEN_BRANCH_REMOVED_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
