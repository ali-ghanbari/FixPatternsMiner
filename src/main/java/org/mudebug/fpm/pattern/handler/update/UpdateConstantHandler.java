package org.mudebug.fpm.pattern.handler.update;

import org.mudebug.fpm.pattern.rules.ConstantReplacementRule;
import org.mudebug.fpm.pattern.rules.Rule;
import org.mudebug.fpm.pattern.rules.UnknownRule;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.CtElement;

public class UpdateConstantHandler extends UpdateHandler {
    protected UpdateConstantHandler(UpdateHandler next) {
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
        if (l1.getType().equals(l2.getType())) {
            return new ConstantReplacementRule(l1.getValue(), l2.getValue());
        }
        return UnknownRule.UNKNOWN_RULE;
    }
}
