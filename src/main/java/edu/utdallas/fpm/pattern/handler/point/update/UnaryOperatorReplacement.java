package edu.utdallas.fpm.pattern.handler.point.update;

import edu.utdallas.fpm.commons.Util;
import edu.utdallas.fpm.pattern.handler.OperationHandler;
import edu.utdallas.fpm.pattern.rules.InterFixUnaryOperatorReplacementRule;
import edu.utdallas.fpm.pattern.rules.Rule;
import edu.utdallas.fpm.pattern.rules.UnaryOperatorReplacementRule;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.declaration.CtElement;

import java.util.Objects;

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
        final CtUnaryOperator uo1 = (CtUnaryOperator) e1;
        final CtUnaryOperator uo2 = (CtUnaryOperator) e2;
        if (Objects.equals(uo1.getType(), uo2.getType())
                && Objects.equals(uo1.getOperand(), uo2.getOperand())) {
            if (Util.sibling(uo1, uo2)) {
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
