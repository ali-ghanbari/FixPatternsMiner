package edu.utdallas.fpm.pattern.rules;

public enum UnknownRule implements Rule {
    UNKNOWN_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
