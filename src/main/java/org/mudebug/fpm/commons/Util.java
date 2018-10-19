package org.mudebug.fpm.commons;

import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;

import java.util.Iterator;

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

    public static boolean containsReturn (final CtStatement stmtBlock) {
        final Iterator<CtElement> it = stmtBlock.descendantIterator();
        while (it.hasNext()) {
            final CtElement element = it.next();
            if (element instanceof CtReturn) {
                return true;
            }
        }
        return false;
    }

//    public static boolean sibling(final CtElement e1, final CtElement e2) {
//        final CtElement p1 = e1.getParent();
//        final CtElement p2 = e2.getParent();
//        if (p1 == null ^ p2 == null) {
//            return false;
//        } else if (p1 == null && p2 == null) {
//            return true;
//        }
//        return p1.equals(p2);
//    }
}
