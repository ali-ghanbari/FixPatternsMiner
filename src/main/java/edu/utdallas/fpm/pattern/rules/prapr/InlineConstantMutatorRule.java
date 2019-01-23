package edu.utdallas.fpm.pattern.rules.prapr;

import edu.utdallas.fpm.pattern.rules.ConstantReplacementRule;
import edu.utdallas.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtLiteral;

public enum InlineConstantMutatorRule implements Rule {
    INLINE_CONSTANT_MUTATOR_RULE;

    public static InlineConstantMutatorRule build(final ConstantReplacementRule crr) {
        final Object src = crr.getSourceLiteral().getValue();
        final CtLiteral literal = crr.getDestinationLiteral();
        final Object dst;
        if (literal != null) {
            dst = literal.getValue();
        } else {
            dst = null;
        }
        if (src != null && dst != null) {
            if (src instanceof Double && dst instanceof Double) {
                final double sd = (Double) src;
                final double dd = (Double) dst;
                if ((sd == 1.D && dd == 2.D) || dd == 1.D) {
                    return INLINE_CONSTANT_MUTATOR_RULE;
                }
            } else if (src instanceof Float && dst instanceof Float) {
                final float sf = (Float) src;
                final float df = (Float) dst;
                if ((sf == 1.F && df == 2.F) || df == 1.F) {
                    return INLINE_CONSTANT_MUTATOR_RULE;
                }
            } else if (src instanceof Long && dst instanceof Long) {
                final long sl = (Long) src;
                final long dl = (Long) dst;
                if (dl == 1 + sl) {
                    return INLINE_CONSTANT_MUTATOR_RULE;
                }
            } else if (src instanceof Integer && dst instanceof Integer) {
                final int si = (Integer) src;
                final int di = (Integer) dst;
                if ((si == 1 && di == 0)
                        || di == 1 + si
                        || (si == Integer.MAX_VALUE && di == Integer.MIN_VALUE)) {
                    return INLINE_CONSTANT_MUTATOR_RULE;
                }
            } else if (src instanceof Short && dst instanceof Short) {
                final short ss = (Short) src;
                final short ds = (Short) dst;
                if ((ss == 1 && ds == 0)
                        || ds == 1 + ss
                        || (ss == Short.MAX_VALUE && ds == Short.MIN_VALUE)) {
                    return INLINE_CONSTANT_MUTATOR_RULE;
                }
            } else if (src instanceof Byte && dst instanceof Byte) {
                final byte sb = (Byte) src;
                final byte db = (Byte) dst;
                if ((sb == 1 && db == 0)
                        || db == 1 + sb
                        || (sb == Byte.MAX_VALUE && db == Byte.MIN_VALUE)) {
                    return INLINE_CONSTANT_MUTATOR_RULE;
                }
            } else if (src instanceof Boolean && dst instanceof Boolean) {
                final boolean sb = (Boolean) src;
                final boolean db = (Boolean) dst;
                if (sb != db) {
                    return INLINE_CONSTANT_MUTATOR_RULE;
                }
            }
        }
        return null;
    }

    @Override
    public String getId() {
        return getClass().getSimpleName();
    }
}
