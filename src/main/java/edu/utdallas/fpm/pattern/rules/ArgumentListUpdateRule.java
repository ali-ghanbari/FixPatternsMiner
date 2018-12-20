package edu.utdallas.fpm.pattern.rules;

public enum ArgumentListUpdateRule implements Rule {
    ARGUMENT_LIST_UPDATE_RULE;

    @Override
    public String getId() {
        /*
        * for now, we don't care which overload we have picked or what data we
        * have used for calling methods with larger number of parameters.
        */
        return this.name();
    }
}
