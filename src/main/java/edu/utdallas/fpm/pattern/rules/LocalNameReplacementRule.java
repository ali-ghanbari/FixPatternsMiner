package edu.utdallas.fpm.pattern.rules;

public enum LocalNameReplacementRule implements Rule {
    LOCAL_NAME_REPLACEMENT_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
