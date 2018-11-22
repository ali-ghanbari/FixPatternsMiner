package org.mudebug.fpm.pattern.rules.prapr_specializations;

import org.mudebug.fpm.pattern.rules.ArgumentPropagatedRule;
import org.mudebug.fpm.pattern.rules.Rule;

public class NakedReceiverMutatorRule implements Rule {
    public static NakedReceiverMutatorRule build(final ArgumentPropagatedRule apr) {
        if (apr.getArgNo() > 0) {
            return null;
        }
        return new NakedReceiverMutatorRule();
    }
}
