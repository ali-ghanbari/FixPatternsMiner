package edu.utdallas.fpm.pattern.rules;

import edu.utdallas.fpm.pattern.rules.commons.SerializableLiteral;
import spoon.reflect.code.CtLiteral;

import static edu.utdallas.fpm.pattern.rules.commons.Util.isNumeric;
import static edu.utdallas.fpm.pattern.rules.commons.Util.getNumericValue;

public class ConstantReplacementRule implements Rule {
    private final SerializableLiteral src;
    private final SerializableLiteral dst; // might be null, e.g., in case of 1 => -1 or -1 => 1

    public ConstantReplacementRule(CtLiteral literal) {
        this(literal, null);
    }

    public ConstantReplacementRule(CtLiteral srcLiteral, CtLiteral dstLiteral) {
        this.src = SerializableLiteral.fromCtLiteral(srcLiteral);
        if (dstLiteral != null) {
            this.dst = SerializableLiteral.fromCtLiteral(dstLiteral);
        } else {
            this.dst = null;
        }
    }

    public SerializableLiteral getSourceLiteral() {
        return src;
    }

    public SerializableLiteral getDestinationLiteral() {
        return dst;
    }

    @Override
    public String getId() {
        final Object sv = this.getSourceLiteral().getValue();
        final Object dv = this.getDestinationLiteral().getValue();
        if (dv == null && isNumeric(sv)) {
            return "NegatedConstant";
        } else {
            if (sv instanceof String && dv instanceof String) {
                return "StringConstantReplacement";
            } else if (isNumeric(sv) && isNumeric(dv)) {
                if (Math.abs(getNumericValue(sv) - getNumericValue(dv)) < 10D) {
                    return String.format("NumericConstantReplacement (%s -> %s)", sv.toString(), dv.toString());
                }
                return "NumericConstantReplacement";
            } else if (sv instanceof Boolean && dv instanceof Boolean) {
                return String.format("BooleanConstantReplacement (%s -> %s)", sv.toString(), dv.toString());
            }
            return this.getClass().getSimpleName();
        }
    }
}
