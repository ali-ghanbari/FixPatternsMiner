package edu.utdallas.fpm.commons;

import edu.utdallas.fpm.pattern.rules.UsagePreference;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.reference.CtTypeReference;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

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
        final CtElement p1 = e1.getParent();
        final CtElement p2 = e2.getParent();
        if (p1 == null || p2 == null) {
            return false;
        } else if (p1 == null && p2 == null) {
            return true;
        }
        final Set<CtElement> set1 = getDescendants(e1);
        final Set<CtElement> set2 = getDescendants(e2);
        return set1.equals(set2);
    }

    private static Set<CtElement> getDescendants(final CtElement element) {
        final Set<CtElement> set = new HashSet<>();
        final Set<CtElement> excluded = new HashSet<>();
        excluded.add(element);
        for (final CtElement e : element.asIterable()) {
            if (e instanceof CtExpression || e instanceof CtStatement) {
                if (!excluded.contains(e)) {
                    final CtElement p = e.getParent();
                    if (excluded.contains(p)) {
                        excluded.add(p);
                    } else {
                        set.add(e);
                    }
                }
            }
        }
        return set;
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
        if (String.format("(%s)", s2).equals(s1)) {
            return true;
        }
        return false;
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
            if (!t1.isPrimitive() && Objects.equals(t1.unbox(), t2)) {
                return true;
            }
            return false;
        }
        return t1.getSimpleName().equals(CtTypeReference.NULL_TYPE_NAME)
                || t2.getSimpleName().equals(CtTypeReference.NULL_TYPE_NAME);
    }

    public static boolean isDefault(final CtLiteral literal) {
        final Object o = literal.getValue();
        if (o instanceof Integer) {
            return ((Integer) o).intValue() == 0;
        } else if (o instanceof Long) {
            return ((Long) o).longValue() == 0;
        } else if (o instanceof Double) {
            return ((Double) o).doubleValue() == 0D;
        } else if (o instanceof Float) {
            return ((Float) o).floatValue() == 0F;
        } else if (o instanceof Short) {
            return ((Short) o).shortValue() == 0;
        } else if (o instanceof Byte) {
            return ((Byte) o).byteValue() == 0;
        }
        return o == null;
    }

//    public static boolean simpleExpression(final CtExpression expression) {
//        return expression instanceof CtLiteral
//                || expression instanceof CtVariableAccess
//                || expression instanceof CtFieldAccess;
//    }
}
