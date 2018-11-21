package org.mudebug.fpm.pattern.rules.prapr_specializations;

import org.mudebug.fpm.pattern.rules.BinaryOperatorReplacementRule;
import org.mudebug.fpm.pattern.rules.Rule;

import static org.mudebug.fpm.pattern.rules.prapr_specializations.Util.isComparison;

public class RORRule implements Rule {
    public static RORRule build(final BinaryOperatorReplacementRule borr) {
        if (isComparison(borr.getSrc()) && isComparison(borr.getDst())) {
            return new RORRule();
        }
        return null;
    }
}
