package edu.utdallas.fpm.pattern.rules;

public enum DerefGuardRule implements Rule {
    DEREF_GUARD_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
