package edu.utdallas.fpm.pattern.rules;

public enum CtorReplacementRule implements Rule {
    CTOR_REPLACEMENT_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
