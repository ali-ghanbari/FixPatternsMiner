package org.mudebug.fpm.pattern.rules.util;

import spoon.reflect.code.CtLiteral;

import java.io.Serializable;

public abstract class SerializableLiteral implements Serializable {
    public static SerializableLiteral fromCtLiteral(final CtLiteral literal) {
        final Object value = literal.getValue();
        if (value == null) {
            return new NullLiteral();
        }
        if (value instanceof Boolean) {
            return new NonNullLiteral<>((Boolean) value);
        } else if (value instanceof Byte) {
            return new NonNullLiteral<>((Byte) value);
        } else if (value instanceof Short) {
            return new NonNullLiteral<>((Short) value);
        } else if (value instanceof Integer) {
            return new NonNullLiteral<>((Integer) value);
        } else if (value instanceof Long) {
            return new NonNullLiteral<>((Long) value);
        } else if (value instanceof Float) {
            return new NonNullLiteral<>((Float) value);
        } else if (value instanceof Double) {
            return new NonNullLiteral<>((Double) value);
        } else if (value instanceof Character) {
            return new NonNullLiteral<>((Character) value);
        } else if (value instanceof String) {
            return new NonNullLiteral<>((String) value);
        }
        return new NonNullLiteral<>(value.toString());
    }

    public abstract Object getValue();

    private static class NullLiteral extends SerializableLiteral {
        @Override
        public Object getValue() {
            return null;
        }
    }

    private static class NonNullLiteral<T> extends SerializableLiteral {
        private final T value;

        public NonNullLiteral(T value) {
            this.value = value;
        }

        @Override
        public Object getValue() {
            return this.value;
        }
    }
}
