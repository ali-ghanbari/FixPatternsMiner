package org.mudebug.fpm.pattern.handler.point.update;

import org.mudebug.fpm.pattern.handler.OperationHandler;
import org.mudebug.fpm.pattern.rules.CtorReplacementRule;
import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtTypeReference;

import java.util.Objects;

import static org.mudebug.fpm.commons.Util.sibling;

public class CtorReplacement extends UpdateHandler {
    public CtorReplacement(OperationHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement e1, CtElement e2) {
        return e1 instanceof CtConstructorCall && e2 instanceof CtConstructorCall;
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        final CtConstructorCall cc1 = (CtConstructorCall) e1;
        final CtConstructorCall cc2 = (CtConstructorCall) e2;
        final CtTypeReference t1 = cc1.getType();
        final CtTypeReference t2 = cc2.getType();
        if (!Objects.equals(t1, t2) && cc1.getArguments().equals(cc2.getArguments())) {
            if (sibling(cc1, cc2)) {
                final String srcQualifiedName = t1.getQualifiedName();
                final String dstQualifiedName = t2.getQualifiedName();
                return new CtorReplacementRule(srcQualifiedName, dstQualifiedName);
            }
        }
        return super.handlePattern(e1, e2);
    }
}
