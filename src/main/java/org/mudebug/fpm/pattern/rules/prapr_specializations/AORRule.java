package org.mudebug.fpm.pattern.rules.prapr_specializations;

import org.mudebug.fpm.pattern.rules.BinaryOperatorReplacementRule;
import org.mudebug.fpm.pattern.rules.Rule;

import static org.mudebug.fpm.pattern.rules.prapr_specializations.Util.isArithmetic;

public class AORRule implements Rule {
    public static AORRule build(final BinaryOperatorReplacementRule borr) {
        if (isArithmetic(borr.getSourceBinaryOperatorKind())
                && isArithmetic(borr.getDestinationBinaryOperatorKind())) {
            return new AORRule();
        }
        return null;
    }
}
