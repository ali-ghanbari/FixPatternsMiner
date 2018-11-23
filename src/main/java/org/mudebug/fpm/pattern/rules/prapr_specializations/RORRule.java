package org.mudebug.fpm.pattern.rules.prapr_specializations;

import org.mudebug.fpm.pattern.rules.BinaryOperatorReplacementRule;
import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.BinaryOperatorKind;

import static org.mudebug.fpm.pattern.rules.prapr_specializations.Util.*;

public class RORRule implements Rule {
    public static RORRule build(final BinaryOperatorReplacementRule borr) {
        final BinaryOperatorKind src = borr.getSourceBinaryOperatorKind();
        final BinaryOperatorKind dst = borr.getDestinationBinaryOperatorKind();
        if (isComparison(src) && isComparison(dst)) {
            if (!isNegated(src, dst)) {
                return new RORRule();
            }
        }
        return null;
    }
}
