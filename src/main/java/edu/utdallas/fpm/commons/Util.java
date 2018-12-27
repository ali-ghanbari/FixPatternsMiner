package edu.utdallas.fpm.commons;

import edu.utdallas.fpm.pattern.rules.UsagePreference;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.reference.CtTypeReference;

import java.util.Iterator;
import java.util.Objects;

public final class Util {
    private Util() {

    }

    public static void panic(final Throwable t) {
        t.printStackTrace();
        System.exit(-1);
    }

    public static CtElement getExecutableContainer(final CtElement element) {
        CtElement cursor = element;
        while (!(cursor instanceof CtExecutable)) {
            cursor = cursor.getParent();
        }
        return cursor;
    }

    public static UsagePreference getReturnStmt(final CtStatement stmtBlock) {
        if (stmtBlock == null) {
            return null;
        }
        final Iterator<CtElement> it = stmtBlock.descendantIterator();
        while (it.hasNext()) {
            final CtElement element = it.next();
            if (element instanceof CtReturn
                    // we should not accept return statements deep inside
                    // some inner block
                    && Objects.equals(element.getParent(), stmtBlock)) {
                CtReturn returnStmt = (CtReturn) element;
                return UsagePreference.fromExpression(returnStmt.getReturnedExpression());
            }
        }
        return null;
    }

    public static boolean sibling(final CtElement e1, final CtElement e2) {
        final SourcePosition sp1 = e1.getPosition();
        final SourcePosition sp2 = e2.getPosition();
        if (sp1 instanceof NoSourcePosition || sp2 instanceof NoSourcePosition) {
            return false;
        }
        return sp1.getSourceStart() == sp2.getSourceStart();
    }

    public static boolean textEquals(final CtExpression e1, final CtExpression e2) {
        if (Objects.equals(e1, e2)) {
            return true;
        }
        if (e1 == null || e2 == null) {
            return false;
        }
        final String s1 = e1.toString();
        final String s2 = e2.toString();
        if (s1.equals(s2)) {
            return true;
        }
        if (String.format("(%s)", s1).equals(s2)) {
            return true;
        }
        return String.format("(%s)", s2).equals(s1);
    }

    public static boolean equalsType(final CtTypeReference t1,
                                     final CtTypeReference t2) {
        if (Objects.equals(t1, t2)) {
            return true;
        }
        if (t1 == null || t2 == null) {
            return false;
        }
        if (t1.isPrimitive() || t2.isPrimitive()) {
            if (!t2.isPrimitive() && Objects.equals(t1, t2.unbox())) {
                return true;
            }
            return !t1.isPrimitive() && Objects.equals(t1.unbox(), t2);
        }
        return t1.getSimpleName().equals(CtTypeReference.NULL_TYPE_NAME)
                || t2.getSimpleName().equals(CtTypeReference.NULL_TYPE_NAME);
    }

    public static boolean isDefault(final CtLiteral literal) {
        final Object o = literal.getValue();
        if (o instanceof Integer) {
            return ((Integer) o) == 0;
        } else if (o instanceof Long) {
            return ((Long) o) == 0;
        } else if (o instanceof Double) {
            return ((Double) o) == 0D;
        } else if (o instanceof Float) {
            return ((Float) o) == 0F;
        } else if (o instanceof Short) {
            return ((Short) o) == 0;
        } else if (o instanceof Byte) {
            return ((Byte) o) == 0;
        }
        return o == null;
    }
}
