package edu.utdallas.fpm.pattern.rules;

public enum RetFieldDerefGuardRule implements Rule {
    RET_FIELD_DEREF_GUARD_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
