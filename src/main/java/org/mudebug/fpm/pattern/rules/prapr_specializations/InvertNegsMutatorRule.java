package org.mudebug.fpm.pattern.rules.prapr_specializations;

import org.mudebug.fpm.pattern.rules.ConstantReplacementRule;
import org.mudebug.fpm.pattern.rules.Rule;

public class InvertNegsMutatorRule implements Rule {
    public static InvertNegsMutatorRule build(final ConstantReplacementRule crr) {
        final Object src = crr.getSourceLiteral().getValue();
        final Object dst = crr.getDestinationLiteral().getValue();
        if (dst == null) {
            if (src instanceof Byte
                    || src instanceof Short
                    || src instanceof Integer
                    || src instanceof Long
                    || src instanceof Float
                    || src instanceof Double) {
                return new InvertNegsMutatorRule();
            }
        }
        return null;
    }
}
