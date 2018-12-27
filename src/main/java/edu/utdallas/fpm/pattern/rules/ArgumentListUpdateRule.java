package edu.utdallas.fpm.pattern.rules;

/**
 * unfortunately, we are unable to determine what kind of expressions are used
 * when using an overload with more parameters. so, in the paper we will say
 * we have made the mining program as general as possible, and in PraPR we have
 * done our best to exhaust all possible cases.
 */
public enum ArgumentListUpdateRule implements Rule {
    ARGUMENT_LIST_UPDATE_RULE;

    @Override
    public String getId() {
        /*
        * for now, we don't care which overload we have picked or what data we
        * have used for calling methods with larger number of parameters.
        */
        return this.getClass().getSimpleName();
    }
}
