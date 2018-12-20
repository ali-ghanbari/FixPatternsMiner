package edu.utdallas.fpm.pattern.rules;

public enum IfStatementRemovedRule implements Rule {
    IF_STATEMENT_REMOVED_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
