package edu.utdallas.fpm.pattern.rules.prapr_specializations;

import edu.utdallas.fpm.pattern.rules.ConstantReplacementRule;
import edu.utdallas.fpm.pattern.rules.Rule;
import edu.utdallas.fpm.pattern.rules.util.SerializableLiteral;

public class InlineConstantMutatorRule implements Rule {
    public static InlineConstantMutatorRule build(final ConstantReplacementRule crr) {
        final Object src = crr.getSourceLiteral().getValue();
        final SerializableLiteral serializableLiteral = crr.getDestinationLiteral();
        final Object dst;
        if (serializableLiteral != null) {
            dst = serializableLiteral.getValue();
        } else {
            dst = null;
        }
        if (src != null && dst != null) {
            if (src instanceof Double && dst instanceof Double) {
                final double sd = ((Double) src).doubleValue();
                final double dd = ((Double) dst).doubleValue();
                if ((sd == 1.D && dd == 2.D) || dd == 1.D) {
                    return new InlineConstantMutatorRule();
                }
            } else if (src instanceof Float && dst instanceof Float) {
                final float sf = ((Float) src).floatValue();
                final float df = ((Float) dst).floatValue();
                if ((sf == 1.F && df == 2.F) || df == 1.F) {
                    return new InlineConstantMutatorRule();
                }
            } else if (src instanceof Long && dst instanceof Long) {
                final long sl = ((Long) src).longValue();
                final long dl = ((Long) dst).longValue();
                if (dl == 1 + sl) {
                    return new InlineConstantMutatorRule();
                }
            } else if (src instanceof Integer && dst instanceof Integer) {
                final int si = ((Integer) src).intValue();
                final int di = ((Integer) dst).intValue();
                if ((si == 1 && di == 0) || di == 1 + si) {
                    return new InlineConstantMutatorRule();
                }
            } else if (src instanceof Short && dst instanceof Short) {
                final short ss = ((Short) src).shortValue();
                final short ds = ((Short) dst).shortValue();
                if ((ss == 1 && ds == 0)
                        || ds == 1 + ss
                        || (ss == Short.MAX_VALUE && ds == Short.MIN_VALUE)) {
                    return new InlineConstantMutatorRule();
                }
            } else if (src instanceof Byte && dst instanceof Byte) {
                final byte sb = ((Byte) src).byteValue();
                final byte db = ((Byte) dst).byteValue();
                if ((sb == 1 && db == 0)
                        || db == 1 + sb
                        || (sb == Short.MAX_VALUE && db == Short.MIN_VALUE)) {
                    return new InlineConstantMutatorRule();
                }
            }
        }
        return null;
    }
}
