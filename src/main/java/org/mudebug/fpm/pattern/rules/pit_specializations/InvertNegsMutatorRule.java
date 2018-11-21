package org.mudebug.fpm.pattern.rules.pit_specializations;

import org.mudebug.fpm.pattern.rules.ConstantReplacementRule;
import org.mudebug.fpm.pattern.rules.Rule;

public class InvertNegsMutatorRule implements Rule {
    public static InvertNegsMutatorRule build(final ConstantReplacementRule crr) {
        final Object src = crr.getSrc();
        final Object dst = crr.getDst();
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
