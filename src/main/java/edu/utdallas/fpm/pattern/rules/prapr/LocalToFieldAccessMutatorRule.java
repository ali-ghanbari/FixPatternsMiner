package edu.utdallas.fpm.pattern.rules.prapr;

import edu.utdallas.fpm.pattern.rules.Rule;

public enum LocalToFieldAccessMutatorRule implements Rule {
    LOCAL_TO_FIELD_ACCESS_MUTATOR_RULE;

    @Override
    public String getId() {
        return getClass().getSimpleName();
    }
}
