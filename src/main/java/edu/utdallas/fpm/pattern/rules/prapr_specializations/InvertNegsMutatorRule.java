package edu.utdallas.fpm.pattern.rules.prapr_specializations;

import edu.utdallas.fpm.pattern.rules.ConstantReplacementRule;
import edu.utdallas.fpm.pattern.rules.Rule;
import edu.utdallas.fpm.pattern.rules.util.SerializableLiteral;

public class InvertNegsMutatorRule implements Rule {
    public static InvertNegsMutatorRule build(final ConstantReplacementRule crr) {
        final Object src = crr.getSourceLiteral().getValue();
        final SerializableLiteral serializableLiteral = crr.getDestinationLiteral();
        final Object dst;
        if (serializableLiteral != null) {
            dst = serializableLiteral.getValue();
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
                return new InvertNegsMutatorRule();
            }
        }
        return null;
    }
}
