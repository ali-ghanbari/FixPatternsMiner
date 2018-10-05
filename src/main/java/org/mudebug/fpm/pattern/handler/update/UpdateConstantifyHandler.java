package org.mudebug.fpm.pattern.handler.update;

import org.mudebug.fpm.pattern.rules.ConstantifyExpressionRule;
import org.mudebug.fpm.pattern.rules.Rule;
import org.mudebug.fpm.pattern.rules.UnknownRule;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.CtElement;

public class UpdateConstantifyHandler extends UpdateHandler {
    public UpdateConstantifyHandler(UpdateHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement e1, CtElement e2) {
        return (nonTrivialExp(e1) && e2 instanceof CtLiteral)
                || ((nonTrivialExp(e2) && e1 instanceof CtLiteral));
    }

    private static boolean nonTrivialExp(CtElement e) {
        return e instanceof CtExpression && !(e instanceof CtLiteral);
    }

    private CtLiteral getLiteral(CtElement e1, CtElement e2) {
        return (CtLiteral) (e1 instanceof CtLiteral ?  e1 :  e2);
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        final CtLiteral lit = getLiteral(e1, e2);
        final CtExpression ex = (CtExpression) (lit == e1 ? e2 : e1);
        if (lit.getType().equals(ex.getType())) {
            return new ConstantifyExpressionRule(ex.getType().getSimpleName(), lit.getValue());
        }
        return UnknownRule.UNKNOWN_RULE;
    }
}
