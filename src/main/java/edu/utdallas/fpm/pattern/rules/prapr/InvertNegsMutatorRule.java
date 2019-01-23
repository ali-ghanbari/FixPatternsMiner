package edu.utdallas.fpm.pattern.rules.prapr;

import edu.utdallas.fpm.pattern.rules.ConstantReplacementRule;
import edu.utdallas.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtLiteral;

public enum InvertNegsMutatorRule implements Rule {
    INVERT_NEGS_MUTATOR_RULE;

    public static InvertNegsMutatorRule build(final ConstantReplacementRule crr) {
        final Object src = crr.getSourceLiteral().getValue();
        final CtLiteral literal = crr.getDestinationLiteral();
        final Object dst;
        if (literal != null) {
            dst = literal.getValue();
        } else {
            dst = null;
        }
        if (dst == null) {
            if (src instanceof Byte
                    || src instanceof Short
                    || src instanceof Integer
                    || src instanceof Long
                    || src instanceof Float
                    || src instanceof Double) {
                return INVERT_NEGS_MUTATOR_RULE;
            }
        }
        return null;
    }

    @Override
    public String getId() {
        return getClass().getSimpleName();
    }
}
