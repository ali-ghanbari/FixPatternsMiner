package edu.utdallas.fpm.pattern.rules;

public enum ThenBranchExtractedRule implements Rule {
    THEN_BRANCH_EXTRACTED_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
