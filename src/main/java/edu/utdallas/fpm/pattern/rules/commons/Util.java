package edu.utdallas.fpm.pattern.rules.commons;

import spoon.reflect.code.CtLiteral;

public final class Util {
    private Util() {

    }

    public static boolean isNumeric(final Object value) {
        return value instanceof Integer || value instanceof Long
                || value instanceof Double || value instanceof Float
                || value instanceof Short || value instanceof Byte;
    }

//    public static Number getNumericValue(final Object value) {
//        if (value instanceof Integer) {
//            return ((Integer) value).intValue();
//        } else if (value instanceof Long) {
//            return ((Long) value).longValue();
//        } else if (value instanceof Double) {
//            return ((Double) value).doubleValue();
//        } else if (value instanceof Float) {
//            return ((Float) value).floatValue();
//        } else if (value instanceof Short) {
//            return ((Short) value).shortValue();
//        } else if (value instanceof Byte) {
//            return ((Byte) value).byteValue();
//        } else {
//            throw new IllegalArgumentException();
//        }
//    }

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
}
