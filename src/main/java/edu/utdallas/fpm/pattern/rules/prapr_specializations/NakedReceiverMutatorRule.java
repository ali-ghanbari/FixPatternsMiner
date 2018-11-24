package edu.utdallas.fpm.pattern.rules.prapr_specializations;

import edu.utdallas.fpm.pattern.rules.ArgumentPropagatedRule;
import edu.utdallas.fpm.pattern.rules.Rule;

public class NakedReceiverMutatorRule implements Rule {
    public static NakedReceiverMutatorRule build(final ArgumentPropagatedRule apr) {
        if (apr.getArgNo() > 0) {
            return null;
        }
        return new NakedReceiverMutatorRule();
    }
}
