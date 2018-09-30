package org.mudebug.fpm.pattern.update;

import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtElement;
import spoon.support.reflect.code.CtInvocationImpl;

public class UpdateInvocationHandler extends UpdateElementHandler {
    public UpdateInvocationHandler(UpdateElementHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandle(CtElement src, CtElement dst) {
        return src instanceof CtInvocation
                && dst instanceof CtInvocation;
    }

    @Override
    protected Rule handle(CtElement src, CtElement dst) {
        final CtInvocation sin = (CtInvocation) src;
        final CtInvocation din = (CtInvocation) dst;

        System.out.println("** " + sin.getExecutable().getSimpleName() + " --- " + sin);
        return null;
    }


//    private String getMethodName(CtInvocation in) {
//        //String res = in.getExecutable().getSimpleName()
//        while (res.startsWith("(")) {
//            res = res.substring(1);
//        }
//        final int firstLP = res.indexOf('(');
//        if (firstLP >= 0) {
//            res = res.substring(0, firstLP);
//        }
//        final int lastPeriod = res.lastIndexOf('.');
//        if (lastPeriod >= 0) {
//            return res.substring(1 + lastPeriod);
//        }
//        return res;
//    }
}
