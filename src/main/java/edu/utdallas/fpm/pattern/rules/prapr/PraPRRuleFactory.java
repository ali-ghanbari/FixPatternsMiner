package edu.utdallas.fpm.pattern.rules.prapr;

import edu.utdallas.fpm.pattern.rules.BinaryOperatorReplacementRule;
import edu.utdallas.fpm.pattern.rules.ConstantReplacementRule;
import edu.utdallas.fpm.pattern.rules.FieldReadToLocalReadRule;
import edu.utdallas.fpm.pattern.rules.FieldWriteToLocalWrite;
import edu.utdallas.fpm.pattern.rules.LocalReadToFieldReadRule;
import edu.utdallas.fpm.pattern.rules.LocalWriteToFieldWriteRule;
import edu.utdallas.fpm.pattern.rules.NegatedConditionalExprRule;
import edu.utdallas.fpm.pattern.rules.Rule;

public final class PraPRRuleFactory {
    private PraPRRuleFactory() {

    }

    public static Rule specialize(final Rule rawRule) {
        if (rawRule instanceof ConstantReplacementRule) {
            final ConstantReplacementRule crr = (ConstantReplacementRule) rawRule;
            Rule specialized = InlineConstantMutatorRule.build(crr);
            if (specialized == null) {
                specialized = InvertNegsMutatorRule.build(crr);
            }
            return specialized;
        } else if (rawRule instanceof BinaryOperatorReplacementRule) {
            final BinaryOperatorReplacementRule borr =
                    (BinaryOperatorReplacementRule) rawRule;
            Rule specialized = MathMutatorRule.build(borr);
            if (specialized == null) {
                specialized = ConditionalBoundaryMutatorRule.build(borr);
                if (specialized == null) {
                    specialized = NegatedConditionalMutatorRule.build(borr);
                }
            }
            return specialized;
        } else if ((rawRule instanceof FieldReadToLocalReadRule)
                || (rawRule instanceof FieldWriteToLocalWrite)) {
            return FieldAccessToLocalAccessMutator.FIELD_ACCESS_TO_LOCAL_ACCESS_MUTATOR;
        } else if ((rawRule instanceof LocalReadToFieldReadRule)
                || (rawRule instanceof LocalWriteToFieldWriteRule)) {
            return LocalToFieldAccessMutatorRule.LOCAL_TO_FIELD_ACCESS_MUTATOR_RULE;
        } else if (rawRule instanceof NegatedConditionalExprRule) {
            final NegatedConditionalExprRule ncer =
                    (NegatedConditionalExprRule) rawRule;
            final Rule specialized = NegatedConditionalMutatorRule.build(ncer);
            return specialized;
        }
        return null;
    }
}
