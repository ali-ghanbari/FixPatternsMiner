package edu.utdallas.fpm.pattern.handler.point.update;

import edu.utdallas.fpm.commons.Util;
import edu.utdallas.fpm.pattern.rules.ConstantReplacementRule;
import edu.utdallas.fpm.pattern.handler.OperationHandler;
import edu.utdallas.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.CtElement;

import java.util.Objects;

public class ConstantReplacement extends UpdateHandler {
    public ConstantReplacement(OperationHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement e1, CtElement e2) {
        return e1 instanceof CtLiteral && e2 instanceof CtLiteral;
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        final CtLiteral l1 = (CtLiteral) e1;
        final CtLiteral l2 = (CtLiteral) e2;
        if (Objects.equals(l1.getType(), l2.getType())) {
            /* according to GumTree paper, we don't need to conduct parent check
             * in case of updates */
            return new ConstantReplacementRule(l1, l2);
        }
        return super.handlePattern(e1, e2);
    }
}
