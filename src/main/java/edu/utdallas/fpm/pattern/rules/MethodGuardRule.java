package edu.utdallas.fpm.pattern.rules;

public enum MethodGuardRule implements Rule {
    METHOD_GUARD_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
