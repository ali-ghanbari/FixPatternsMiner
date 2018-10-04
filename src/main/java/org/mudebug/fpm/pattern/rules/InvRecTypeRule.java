package org.mudebug.fpm.pattern.rules;

import spoon.reflect.reference.CtTypeReference;

/**
 * r1.meth1(args1)
 * r2.meth2(args1)
 *
 * where meth1 == meth2 but args1 == args2.
 * r1 and r2 are of different type.
 */
public class InvRecTypeRule implements Rule {
    private final CtTypeReference srcRecType;
    private final CtTypeReference dstRecType;

    public InvRecTypeRule(CtTypeReference srcRecType, CtTypeReference dstRecType) {
        this.srcRecType = srcRecType;
        this.dstRecType = dstRecType;
    }
}
