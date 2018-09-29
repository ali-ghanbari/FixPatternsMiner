package org.mudebug.fpm.commons;

public final class Util {
    private Util() {

    }

    public static void panic(final Throwable t) {
        t.printStackTrace();
        System.exit(-1);
    }
}
