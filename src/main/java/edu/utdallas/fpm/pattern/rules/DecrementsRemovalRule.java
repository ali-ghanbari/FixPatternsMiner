package edu.utdallas.fpm.pattern.rules;

public enum DecrementsRemovalRule implements Rule {
    DECREMENTS_REMOVAL_RULE;


    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
