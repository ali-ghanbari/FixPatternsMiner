package edu.utdallas.fpm.pattern.rules;

public enum CtorCallRemovalRule implements Rule {
    CTOR_CALL_REMOVAL_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
