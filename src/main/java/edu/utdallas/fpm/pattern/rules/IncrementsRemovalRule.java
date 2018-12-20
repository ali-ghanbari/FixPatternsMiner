package edu.utdallas.fpm.pattern.rules;

public enum IncrementsRemovalRule implements Rule {
    INCREMENTS_REMOVAL_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
