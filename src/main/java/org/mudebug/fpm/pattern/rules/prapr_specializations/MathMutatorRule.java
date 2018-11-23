package org.mudebug.fpm.pattern.rules.prapr_specializations;

import org.mudebug.fpm.pattern.rules.BinaryOperatorReplacementRule;
import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.BinaryOperatorKind;

import static spoon.reflect.code.BinaryOperatorKind.*;

public class MathMutatorRule implements Rule {
    public static MathMutatorRule build(final BinaryOperatorReplacementRule borr) {
        final BinaryOperatorKind src = borr.getSourceBinaryOperatorKind();
        final BinaryOperatorKind dst = borr.getDestinationBinaryOperatorKind();
        if ((src == PLUS && dst == MINUS)
                || (src == MINUS && dst == PLUS)
                || (src == MUL && dst == DIV)
                || (src == DIV && dst == MUL)
                || (src == BITOR && dst == BITAND)
                || (src == BITAND && dst == BITOR)
                || (src == MOD && dst == MUL)
                || (src == BITXOR && dst == BITAND)
                || (src == SL && dst == SR)
                || (src == SR && dst == SL)
                || (src == USR && dst == SL)) {
            return new MathMutatorRule();
        }
        return null;
    }
}
