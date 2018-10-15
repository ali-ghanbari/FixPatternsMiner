package org.mudebug.fpm.pattern.handler.point.update;

import org.mudebug.fpm.pattern.handler.OperationHandler;
import org.mudebug.fpm.pattern.rules.InterFixUnaryOperatorReplacementRule;
import org.mudebug.fpm.pattern.rules.Rule;
import org.mudebug.fpm.pattern.rules.UnaryOperatorReplacementRule;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.declaration.CtElement;

/**
 * Takes care of patterns like
 *  a++ -> a--
 * Please note that since a++ and ++a have different
 * semantics, we also capture changes line
 *  a++ -> ++a.
 */
public class UnaryOperatorReplacement extends UpdateHandler {
    public UnaryOperatorReplacement(OperationHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement e1, CtElement e2) {
        return e1 instanceof CtUnaryOperator && e2 instanceof CtUnaryOperator;
    }

    private boolean interFix(UnaryOperatorKind k1, UnaryOperatorKind k2) {
        return (k1 == UnaryOperatorKind.POSTDEC && k2 == UnaryOperatorKind.PREDEC)
                || (k2 == UnaryOperatorKind.POSTDEC && k1 == UnaryOperatorKind.PREDEC)
                || (k1 == UnaryOperatorKind.POSTINC && k2 == UnaryOperatorKind.PREINC)
                || (k2 == UnaryOperatorKind.POSTINC && k1 == UnaryOperatorKind.PREINC);
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        if (e1 instanceof CtUnaryOperator && e2 instanceof CtUnaryOperator) {
            final CtUnaryOperator uo1 = (CtUnaryOperator) e1;
            final CtUnaryOperator uo2 = (CtUnaryOperator) e2;
            if (uo1.getType().equals(uo2.getType()) && uo1.getOperand().equals(uo2.getOperand())) {
                if (interFix(uo1.getKind(), uo2.getKind())) {
                    return new InterFixUnaryOperatorReplacementRule(uo1.getKind(), uo2.getKind());
                } else { // since operands are same, the operators are guaranteed to be different
                    return new UnaryOperatorReplacementRule(uo1.getKind(), uo2.getKind());
                }
            }
        }
        return super.handlePattern(e1, e2);
    }
}
