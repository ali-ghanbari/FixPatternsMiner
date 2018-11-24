package edu.utdallas.fpm.pattern.rules.prapr_specializations;

import edu.utdallas.fpm.pattern.rules.BinaryOperatorReplacementRule;
import edu.utdallas.fpm.pattern.rules.Rule;

public class AORRule implements Rule {
    public static AORRule build(final BinaryOperatorReplacementRule borr) {
        if (Util.isArithmetic(borr.getSourceBinaryOperatorKind())
                && Util.isArithmetic(borr.getDestinationBinaryOperatorKind())) {
            return new AORRule();
        }
        return null;
    }
}
