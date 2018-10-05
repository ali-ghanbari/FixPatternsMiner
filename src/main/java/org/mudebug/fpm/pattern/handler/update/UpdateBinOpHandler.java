package org.mudebug.fpm.pattern.handler.update;

import org.mudebug.fpm.pattern.handler.OperationHandler;
import org.mudebug.fpm.pattern.rules.BinOpReplacementRule;
import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtElement;

import java.util.List;

/**
 * Responsible for things like this:
 *  a + b -> a * b
 *  a - b -> Math.min(a, b)
 *  and vice versa.
 */
public class UpdateBinOpHandler extends UpdateHandler {
    public UpdateBinOpHandler(OperationHandler next) {
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

    private CtInvocation getInvocation(CtElement e1, CtElement e2) {
        return (e1 instanceof CtInvocation) ? (CtInvocation) e1 : (CtInvocation) e2;
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
        } else {
            final CtBinaryOperator bo = getBinOp(e1, e2);
            final CtInvocation in = getInvocation(e1, e2);
            if (bo.getType().equals(in.getType())) {
                final List<CtExpression> args = in.getArguments();
                if (args.size() == 2) {
                    if ((args.get(0).equals(bo.getLeftHandOperand()) && args.get(1).equals(bo.getRightHandOperand()))
                        || (args.get(1).equals(bo.getLeftHandOperand()) && args.get(0).equals(bo.getRightHandOperand()))) {
                        if (bo == e1) {
                            return new BinOpReplacementRule(bo.getKind(), in.getExecutable().getSignature());
                        }
                        return new BinOpReplacementRule(in.getExecutable().getSignature(), bo.getKind());
                    }
                }
            }
        }
        return super.handlePattern(e1, e2);
    }
}
