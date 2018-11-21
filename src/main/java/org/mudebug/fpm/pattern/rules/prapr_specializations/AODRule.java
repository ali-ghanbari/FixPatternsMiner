package org.mudebug.fpm.pattern.rules.prapr_specializations;

import org.mudebug.fpm.pattern.rules.BinaryOperatorDeletedRule;
import org.mudebug.fpm.pattern.rules.Rule;

import static org.mudebug.fpm.pattern.rules.prapr_specializations.Util.isArithmetic;

public class AODRule implements Rule {
    public static AODRule build(final BinaryOperatorDeletedRule dobr) {
        if (isArithmetic(dobr.getKind())) {
            // warning: this might have some noise
            //          e.g. "a" + "b" --> "a" will
            //          also be considered as AOD
            return new AODRule();
        }
        return null;
    }
}
