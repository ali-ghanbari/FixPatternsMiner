package edu.utdallas.fpm.pattern.rules;

public enum SimpleMethCallRemovalRule implements Rule {
    SIMPLE_METH_CALL_REMOVAL_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
