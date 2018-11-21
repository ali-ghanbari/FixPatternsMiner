package org.mudebug.fpm.pattern.rules.prapr_specializations;

import spoon.reflect.code.BinaryOperatorKind;

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
}
