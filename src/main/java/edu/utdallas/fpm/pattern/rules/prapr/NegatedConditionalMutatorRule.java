package edu.utdallas.fpm.pattern.rules.prapr;

import edu.utdallas.fpm.pattern.rules.BinaryOperatorReplacementRule;
import edu.utdallas.fpm.pattern.rules.NegatedConditionalExprRule;
import edu.utdallas.fpm.pattern.rules.Rule;
import edu.utdallas.fpm.pattern.rules.commons.Util;
import spoon.reflect.code.BinaryOperatorKind;

public enum NegatedConditionalMutatorRule implements Rule {
    NEGATED_CONDITIONAL_MUTATOR_RULE;

    public static NegatedConditionalMutatorRule build(final NegatedConditionalExprRule ncer) {
        return NEGATED_CONDITIONAL_MUTATOR_RULE;
    }

    public static NegatedConditionalMutatorRule build(final BinaryOperatorReplacementRule borr) {
        final BinaryOperatorKind src = borr.getSourceBinaryOperatorKind();
        final BinaryOperatorKind dst = borr.getDestinationBinaryOperatorKind();
        if (Util.isComparison(src) && Util.isComparison(dst)) {
            if (Util.isNegated(src, dst)) {
                return NEGATED_CONDITIONAL_MUTATOR_RULE;
            }
        }
        return null;
    }

    @Override
    public String getId() {
        return getClass().getSimpleName();
    }
}
