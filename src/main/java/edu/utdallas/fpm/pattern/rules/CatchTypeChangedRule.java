package edu.utdallas.fpm.pattern.rules;

public enum CatchTypeChangedRule implements Rule {
    CATCH_TYPE_CHANGED_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
