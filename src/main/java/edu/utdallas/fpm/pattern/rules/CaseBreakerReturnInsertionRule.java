package edu.utdallas.fpm.pattern.rules;

public enum CaseBreakerReturnInsertionRule implements Rule {
    CASE_BREAKER_RETURN_INSERTION_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
