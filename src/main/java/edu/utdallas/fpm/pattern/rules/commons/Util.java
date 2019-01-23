package edu.utdallas.fpm.pattern.rules.commons;

import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtLiteral;

import static spoon.reflect.code.BinaryOperatorKind.*;

public final class Util {
    private Util() {

    }

    public static boolean isNumeric(final Object value) {
        return value instanceof Integer || value instanceof Long
                || value instanceof Double || value instanceof Float
                || value instanceof Short || value instanceof Byte;
    }

    public static boolean isReasonableNumericValue(final Object value) {
        if (isNumeric(value)) {
            final double doubleValue = ((Number) value).doubleValue();
            return -5D <= doubleValue && doubleValue <= 5D;
        } else {
            return false;
        }
    }

    public static boolean isReasonableInterval(final Number lb,
                                               final Number ub) {
        final double doubleValue = ub.doubleValue() - lb.doubleValue();
        return -5D <= doubleValue && doubleValue <= 5D;
    }

    public static String renderLiteral(final CtLiteral literal) {
        final Object value = literal.getValue();
        final String strVal;
        if (value == null) {
            strVal = "NULL";
        } else if (isReasonableNumericValue(value) || value instanceof Boolean) {
            strVal = value.toString();
        } else {
            strVal = "SOME " + value.getClass().getSimpleName();
        }
        return strVal;
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
