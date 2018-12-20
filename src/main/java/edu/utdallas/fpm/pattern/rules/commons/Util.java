package edu.utdallas.fpm.pattern.rules.commons;

public final class Util {
    private Util() {

    }

    public static boolean isNumeric(final Object value) {
        return value instanceof Integer || value instanceof Long
                || value instanceof Double || value instanceof Float
                || value instanceof Short || value instanceof Byte;
    }

    public static double getNumericValue(final Object value) {
        if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        } else if (value instanceof Long) {
            return ((Long) value).doubleValue();
        } else if (value instanceof Double) {
            return ((Double) value).doubleValue();
        } else if (value instanceof Float) {
            return ((Float) value).doubleValue();
        } else if (value instanceof Short) {
            return ((Short) value).doubleValue();
        } else if (value instanceof Byte) {
            return ((Byte) value).doubleValue();
        } else {
            throw new IllegalArgumentException();
        }
    }
}
