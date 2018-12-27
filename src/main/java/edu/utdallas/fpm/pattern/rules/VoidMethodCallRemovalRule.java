package edu.utdallas.fpm.pattern.rules;

public enum VoidMethodCallRemovalRule implements Rule {
    VOID_METHOD_CALL_REMOVAL_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
