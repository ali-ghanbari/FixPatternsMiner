package edu.utdallas.fpm.pattern.rules.prapr_specializations;

import edu.utdallas.fpm.pattern.rules.NegatedConditionalExprRule;
import edu.utdallas.fpm.pattern.rules.BinaryOperatorReplacementRule;
import edu.utdallas.fpm.pattern.rules.Rule;
import spoon.reflect.code.BinaryOperatorKind;

public class NegatedConditionalMutatorRule implements Rule {
    public static NegatedConditionalMutatorRule build(final NegatedConditionalExprRule ncer) {
        return new NegatedConditionalMutatorRule();
    }

    public static NegatedConditionalMutatorRule build(final BinaryOperatorReplacementRule borr) {
        final BinaryOperatorKind src = borr.getSourceBinaryOperatorKind();
        final BinaryOperatorKind dst = borr.getDestinationBinaryOperatorKind();
        if (Util.isComparison(src) && Util.isComparison(dst)) {
            if (Util.isNegated(src, dst)) {
                return new NegatedConditionalMutatorRule();
            }
        }
        return null;
    }
}
