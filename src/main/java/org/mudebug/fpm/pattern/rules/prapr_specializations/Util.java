package org.mudebug.fpm.pattern.rules.prapr_specializations;

import spoon.reflect.code.BinaryOperatorKind;

import static spoon.reflect.code.BinaryOperatorKind.*;

public final class Util {
    private Util() {

    }

    public static boolean isArithmetic(final BinaryOperatorKind bok) {
        switch (bok) {
            case PLUS:
            case MINUS:
            case MUL:
            case DIV:
            case MOD:
            case SL:
            case SR:
            case USR:
            case BITOR:
            case BITAND:
            case BITXOR:
                return true;
        }
        return false;
    }

    public static boolean isComparison(final BinaryOperatorKind bok) {
        switch (bok) {
            case EQ:
            case GE:
            case GT:
            case LE:
            case LT:
            case NE:
                return true;
        }
        return false;
    }

    public static boolean isNegated(final BinaryOperatorKind o1,
                                    final BinaryOperatorKind o2) {
        if ((o1 == EQ && o2 == NE)
                || (o1 == NE && o2 == EQ)
                || (o1 == LE && o2 == GT)
                || (o1 == GT && o2 == LE)
                || (o1 == LT && o2 == GE)
                || (o1 == GE && o2 == LT)) {
            return true;
        }
        return false;
    }
}
