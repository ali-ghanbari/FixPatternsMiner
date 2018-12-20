package edu.utdallas.fpm.pattern.rules;

import edu.utdallas.fpm.pattern.rules.util.SerializableLiteral;
import spoon.reflect.code.CtLiteral;

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
        final Object srcVal = this.src.getValue();
        final Object dstVal = this.dst.getValue();

    }
}
