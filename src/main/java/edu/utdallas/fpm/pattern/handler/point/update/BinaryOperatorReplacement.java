package edu.utdallas.fpm.pattern.handler.point.update;

import edu.utdallas.fpm.commons.Util;
import edu.utdallas.fpm.pattern.handler.OperationHandler;
import edu.utdallas.fpm.pattern.rules.BinaryOperatorReplacementRule;
import edu.utdallas.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.declaration.CtElement;

import java.util.Objects;

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
                && Objects.equals(bo1.getType(), bo2.getType())
                && Objects.equals(bo1.getLeftHandOperand(), bo2.getLeftHandOperand())
                && Objects.equals(bo1.getRightHandOperand(), bo2.getRightHandOperand())) {
            if (Util.sibling(bo1, bo2)) {
                return new BinaryOperatorReplacementRule(bo1.getKind(), bo2.getKind());
            }
        }
        return super.handlePattern(e1, e2);
    }
}
