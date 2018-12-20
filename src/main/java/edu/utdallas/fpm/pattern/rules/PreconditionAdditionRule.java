package edu.utdallas.fpm.pattern.rules;

public enum PreconditionAdditionRule implements Rule {
    PRECONDITION_ADDITION_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
