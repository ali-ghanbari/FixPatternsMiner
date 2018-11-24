package edu.utdallas.fpm.pattern.rules.prapr_specializations;

import edu.utdallas.fpm.pattern.rules.BinaryOperatorReplacementRule;
import edu.utdallas.fpm.pattern.rules.Rule;
import spoon.reflect.code.BinaryOperatorKind;

public class RORRule implements Rule {
    public static RORRule build(final BinaryOperatorReplacementRule borr) {
        final BinaryOperatorKind src = borr.getSourceBinaryOperatorKind();
        final BinaryOperatorKind dst = borr.getDestinationBinaryOperatorKind();
        if (Util.isComparison(src) && Util.isComparison(dst)) {
            if (!Util.isNegated(src, dst)) {
                return new RORRule();
            }
        }
        return null;
    }
}
