package edu.utdallas.fpm.pattern.rules.prapr_specializations;

import edu.utdallas.fpm.pattern.rules.BinaryOperatorDeletedRule;
import edu.utdallas.fpm.pattern.rules.Rule;

public class AODRule implements Rule {
    public static AODRule build(final BinaryOperatorDeletedRule dobr) {
        if (Util.isArithmetic(dobr.getDeletedBinaryOperatorKind())) {
            // warning: this might have some noise
            //          e.g. "a" + "b" --> "a" will
            //          also be considered as AOD
            return new AODRule();
        }
        return null;
    }
}
