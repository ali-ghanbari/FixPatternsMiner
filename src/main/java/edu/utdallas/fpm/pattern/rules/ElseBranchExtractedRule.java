package edu.utdallas.fpm.pattern.rules;

public enum ElseBranchExtractedRule implements Rule {
    ELSE_BRANCH_EXTRACTED_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
