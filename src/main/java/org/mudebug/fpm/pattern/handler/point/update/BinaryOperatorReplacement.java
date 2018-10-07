package org.mudebug.fpm.pattern.handler.point.update;

import org.mudebug.fpm.pattern.handler.OperationHandler;
import org.mudebug.fpm.pattern.rules.BinOpReplacementRule;
import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.declaration.CtElement;

/**
 * Responsible for things like this:
 *  a + b -> a * b
 *  and vice versa.
 *  Please note that cases like a + b -> Math.max(a, b) is
 *  not handled by this one
 */
public class BinaryOperatorReplacement extends UpdateHandler {
    public BinaryOperatorReplacement(OperationHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement e1, CtElement e2) {
        return e1 instanceof CtBinaryOperator && e2 instanceof CtBinaryOperator;
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        final CtBinaryOperator bo1 = (CtBinaryOperator) e1;
        final CtBinaryOperator bo2 = (CtBinaryOperator) e2;
        if (bo1.getKind() != bo2.getKind()
                && bo1.getType().equals(bo2.getType())
                && bo1.getLeftHandOperand().equals(bo2.getLeftHandOperand())
                && bo1.getRightHandOperand().equals(bo2.getRightHandOperand())) {
            return new BinOpReplacementRule(bo1.getKind(), bo2.getKind());
        }
        return super.handlePattern(e1, e2);
    }
}
