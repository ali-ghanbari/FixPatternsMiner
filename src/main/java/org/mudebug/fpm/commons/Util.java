package org.mudebug.fpm.commons;

import spoon.reflect.declaration.CtElement;

public final class Util {
    private Util() {

    }

    public static void panic(final Throwable t) {
        t.printStackTrace();
        System.exit(-1);
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
