package org.mudebug.fpm.pattern.rules.prapr_specializations;

import org.mudebug.fpm.pattern.rules.*;

public final class Factory {
    private static Factory ourInstance = new Factory();

    public static synchronized Factory getInstance() {
        return ourInstance;
    }

    private Factory() {

    }

    /* returns null if not specializable */
    public Rule specialize(final Rule rule) {
        Rule specialized = null;
        if (rule instanceof ConstantReplacementRule) {
            final ConstantReplacementRule crr = (ConstantReplacementRule) rule;
            specialized = InlineConstantMutatorRule.build(crr);
            if (specialized == null) {
                specialized = InvertNegsMutatorRule.build(crr);
            }
        } else if (rule instanceof BinaryOperatorDeletedRule) {
            final BinaryOperatorDeletedRule dobr = (BinaryOperatorDeletedRule) rule;
            specialized = AODRule.build(dobr);
        } else if (rule instanceof BinaryOperatorReplacementRule) {
            final BinaryOperatorReplacementRule borr =
                    (BinaryOperatorReplacementRule) rule;
            specialized = MathMutatorRule.build(borr);
            if (specialized == null) {
                specialized = AORRule.build(borr);
                if (specialized == null) {
                    specialized = ConditionalBoundaryMutatorRule.build(borr);
                    if (specialized == null) {
                        specialized = RORRule.build(borr);
                        if (specialized == null) {
                            specialized = NegatedConditionalMutatorRule.build(borr);
                        }
                    }
                }
            }
        } else if ((rule instanceof FieldReadToLocalReadRule)
                || (rule instanceof FieldWriteToLocalWrite)) {
            specialized = new FieldAccessToLocalAccessMutatorRule();
        } else if ((rule instanceof LocalReadToFieldReadRule)
                || (rule instanceof LocalWriteToFieldWriteRule)) {
            specialized = new LocalToFieldAccessMutatorRule();
        } else if (rule instanceof NegatedConditionalExprRule) {
            final NegatedConditionalExprRule ncer = (NegatedConditionalExprRule) rule;
            specialized = NegatedConditionalMutatorRule.build(ncer);
        } else if (rule instanceof ArgumentPropagatedRule) {
            final ArgumentPropagatedRule apr = (ArgumentPropagatedRule) rule;
            specialized = NakedReceiverMutatorRule.build(apr);
        }
        return specialized;
    }
}
