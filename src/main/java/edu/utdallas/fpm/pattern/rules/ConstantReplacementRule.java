package edu.utdallas.fpm.pattern.rules;

import spoon.reflect.code.CtLiteral;

import static edu.utdallas.fpm.pattern.rules.commons.Util.isNumeric;
import static edu.utdallas.fpm.pattern.rules.commons.Util.isReasonableInterval;

public class ConstantReplacementRule implements Rule {
    private final CtLiteral src;
    private final CtLiteral dst; // might be null, e.g., in case of 1 => -1 or -1 => 1

    public ConstantReplacementRule(CtLiteral literal) {
        this(literal, null);
    }

    public ConstantReplacementRule(CtLiteral src, CtLiteral dst) {
        this.src = src;
        this.dst = dst;
    }

    public CtLiteral getSourceLiteral() {
        return src;
    }

    public CtLiteral getDestinationLiteral() {
        return dst;
    }

    @Override
    public String getId() {
        final Object sv = this.src.getValue();
        final Object dv = this.dst == null ? null : this.dst.getValue();
        if (dv == null && isNumeric(sv)) {
            return "NegatedConstant";
        } else {
            if (isNumeric(sv) && isNumeric(dv)) {
                if (isReasonableInterval((Number) sv, (Number) dv)) {
                    return String.format("NumericConstantReplacement (%s -> %s)",
                            sv.toString(), dv.toString());
                }
                return "NumericConstantReplacement";
            } else if (sv instanceof Boolean && dv instanceof Boolean) {
                return String.format("BooleanConstantReplacement (%s -> %s)",
                        sv.toString(), dv.toString());
            }
            return this.getClass().getSimpleName(); // this includes string constants
        }
    }
}
