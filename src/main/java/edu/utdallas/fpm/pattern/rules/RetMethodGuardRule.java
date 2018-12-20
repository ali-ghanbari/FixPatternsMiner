package edu.utdallas.fpm.pattern.rules;

public enum RetMethodGuardRule implements Rule {
    RET_METHOD_GUARD_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
