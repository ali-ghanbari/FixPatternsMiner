package edu.utdallas.fpm.pattern.rules;

public enum CaseRemovalRule implements Rule {
    CASE_REMOVAL_RULE;

    @Override
    public String getId() {
        return this.name();
    }
}
