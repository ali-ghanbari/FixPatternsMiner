package org.mudebug.fpm.pattern.rules.prapr_specializations;

import org.mudebug.fpm.pattern.rules.BinaryOperatorReplacementRule;
import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.BinaryOperatorKind;

import static spoon.reflect.code.BinaryOperatorKind.*;

public class ConditionalBoundaryMutatorRule implements Rule {
    public static ConditionalBoundaryMutatorRule build(final BinaryOperatorReplacementRule borr) {
        final BinaryOperatorKind src = borr.getSrc();
        final BinaryOperatorKind dst = borr.getDst();
        if ((src == LE && dst == LT)
                || (src == LT && dst == LE)
                || (src == GE && dst == GT)
                || (src == GT && dst == GE)) {
            return new ConditionalBoundaryMutatorRule();
        }
        return null;
    }
}
