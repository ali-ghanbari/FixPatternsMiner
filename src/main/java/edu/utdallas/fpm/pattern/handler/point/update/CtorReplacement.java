package edu.utdallas.fpm.pattern.handler.point.update;

import edu.utdallas.fpm.pattern.handler.OperationHandler;
import edu.utdallas.fpm.pattern.rules.CtorReplacementRule;
import edu.utdallas.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtTypeReference;

import java.util.Objects;

import static edu.utdallas.fpm.commons.Util.sibling;

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
        if (!Objects.equals(t1, t2)) {
            if (Objects.equals(cc1.getArguments(), cc2.getArguments())) {
                // according to GumTree paper, we don't need to do parent check for updates
                if (sibling(cc1, cc2)) {
                    return CtorReplacementRule.CTOR_REPLACEMENT_RULE;
                }
            }
        }
        return super.handlePattern(e1, e2);
    }
}
