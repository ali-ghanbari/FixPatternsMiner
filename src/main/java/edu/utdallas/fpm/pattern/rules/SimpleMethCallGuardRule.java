package edu.utdallas.fpm.pattern.rules;

public enum SimpleMethCallGuardRule implements Rule {
    SIMPLE_METH_CALL_GUARD_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
