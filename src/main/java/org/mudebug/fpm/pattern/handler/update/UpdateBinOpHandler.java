package org.mudebug.fpm.pattern.handler.update;

import org.mudebug.fpm.pattern.rules.BinOpReplacementRule;
import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtElement;

/**
 * Responsible for things like this:
 *  a + b -> a * b
 *  a - b -> Math.min(a, b)
 *  and vice versa.
 */
public class UpdateBinOpHandler extends UpdateHandler {
    public UpdateBinOpHandler(UpdateHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement e1, CtElement e2) {
        return (e1 instanceof CtBinaryOperator && e2 instanceof CtBinaryOperator)
                || (e1 instanceof CtBinaryOperator && e2 instanceof CtInvocation)
                || (e2 instanceof CtBinaryOperator && e1 instanceof CtInvocation);
    }

    private CtBinaryOperator getBinOp(CtElement e1, CtElement e2) {
        return (e1 instanceof CtBinaryOperator) ? (CtBinaryOperator) e1 : (CtBinaryOperator) e2;
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        if (e1 instanceof CtBinaryOperator && e2 instanceof CtBinaryOperator) {
            final CtBinaryOperator bo1 = (CtBinaryOperator) e1;
            final CtBinaryOperator bo2 = (CtBinaryOperator) e2;
            if (bo1.getKind() != bo2.getKind()
                    && bo1.getType().equals(bo2.getType())
                    && bo1.getLeftHandOperand().equals(bo2.getLeftHandOperand())
                    && bo1.getRightHandOperand().equals(bo2.getRightHandOperand())) {
                return new BinOpReplacementRule(bo1.getKind(), bo2.getKind());
            }
        }
        return null;
    }
}
