package edu.utdallas.fpm.pattern.rules.prapr_specializations;

import edu.utdallas.fpm.pattern.rules.BinaryOperatorReplacementRule;
import edu.utdallas.fpm.pattern.rules.Rule;
import spoon.reflect.code.BinaryOperatorKind;

import static spoon.reflect.code.BinaryOperatorKind.*;

public class ConditionalBoundaryMutatorRule implements Rule {
    public static ConditionalBoundaryMutatorRule build(final BinaryOperatorReplacementRule borr) {
        final BinaryOperatorKind src = borr.getSourceBinaryOperatorKind();
        final BinaryOperatorKind dst = borr.getDestinationBinaryOperatorKind();
        if ((src == LE && dst == LT)
                || (src == LT && dst == LE)
                || (src == GE && dst == GT)
                || (src == GT && dst == GE)) {
            return new ConditionalBoundaryMutatorRule();
        }
        return null;
    }
}
