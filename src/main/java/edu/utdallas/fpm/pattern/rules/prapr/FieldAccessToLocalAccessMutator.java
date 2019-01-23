package edu.utdallas.fpm.pattern.rules.prapr;

import edu.utdallas.fpm.pattern.rules.Rule;

public enum FieldAccessToLocalAccessMutator implements Rule {
    FIELD_ACCESS_TO_LOCAL_ACCESS_MUTATOR;

    @Override
    public String getId() {
        return getClass().getSimpleName();
    }
}
