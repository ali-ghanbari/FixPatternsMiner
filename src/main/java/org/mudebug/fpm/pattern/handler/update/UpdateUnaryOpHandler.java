package org.mudebug.fpm.pattern.handler.update;

import org.mudebug.fpm.pattern.rules.InterFixUnaryOpRepRule;
import org.mudebug.fpm.pattern.rules.Rule;
import org.mudebug.fpm.pattern.rules.UnaryOpReplacementRule;
import org.mudebug.fpm.pattern.rules.UnknownRule;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.declaration.CtElement;

import java.util.List;

/**
 * Takes care of patterns like
 *  a++ -> a--
 * Please note that since a++ and ++a have different
 * semantics, we also capture changes line
 *  a++ -> ++a.
 */
public class UpdateUnaryOpHandler extends UpdateHandler {
    protected UpdateUnaryOpHandler(UpdateHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement e1, CtElement e2) {
        return (e1 instanceof CtUnaryOperator && e2 instanceof CtUnaryOperator)
                || (e1 instanceof CtUnaryOperator && e2 instanceof CtInvocation)
                || (e1 instanceof CtInvocation && e2 instanceof CtUnaryOperator);
    }

    private boolean interFix(UnaryOperatorKind k1, UnaryOperatorKind k2) {
        return (k1 == UnaryOperatorKind.POSTDEC && k2 == UnaryOperatorKind.PREDEC)
                || (k2 == UnaryOperatorKind.POSTDEC && k1 == UnaryOperatorKind.PREDEC)
                || (k1 == UnaryOperatorKind.POSTINC && k2 == UnaryOperatorKind.PREINC)
                || (k2 == UnaryOperatorKind.POSTINC && k1 == UnaryOperatorKind.PREINC);
    }

    private CtUnaryOperator getUnOp(CtElement e1, CtElement e2) {
        return (e1 instanceof CtUnaryOperator) ? (CtUnaryOperator) e1 : (CtUnaryOperator) e2;
    }

    private CtInvocation getInvocation(CtElement e1, CtElement e2) {
        return (e1 instanceof CtInvocation) ? (CtInvocation) e1 : (CtInvocation) e2;
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        if (e1 instanceof CtUnaryOperator && e2 instanceof CtUnaryOperator) {
            final CtUnaryOperator uo1 = (CtUnaryOperator) e1;
            final CtUnaryOperator uo2 = (CtUnaryOperator) e2;
            if (uo1.getType().equals(uo2.getType()) && uo1.getOperand().equals(uo2.getOperand())) {
                if (interFix(uo1.getKind(), uo2.getKind())) {
                    System.out.println("111111111111111");
                    System.out.println(uo1.toString() + " --> " + uo2.toString());
                    System.out.println("111111111111111");
                    return new InterFixUnaryOpRepRule(uo1.getKind(), uo2.getKind());
                } else { // since operands are same, the operators are guaranteed to be different
                    System.out.println("222222222222222");
                    System.out.println(uo1.toString() + " --> " + uo2.toString());
                    System.out.println("222222222222222");
                    return new UnaryOpReplacementRule(uo1.getKind(), uo2.getKind());
                }
            }
        } else {
            final CtUnaryOperator uo = getUnOp(e1, e2);
            final CtInvocation in = getInvocation(e1, e2);
            if (uo.getType().equals(in.getType())) {
                final List<CtExpression> args = in.getArguments();
                if (args.size() == 1 && args.get(0).equals(uo.getOperand())) {
                    if (uo == e1) {
                        System.out.println("333333333333333");
                        System.out.println(uo.toString() + " --> " + in.toString());
                        System.out.println("333333333333333");
                        return new UnaryOpReplacementRule(uo.getKind(), in.getExecutable().getSignature());
                    }
                    System.out.println("444444444444444");
                    System.out.println(in.toString() + " --> " + uo.toString());
                    System.out.println("444444444444444");
                    return new UnaryOpReplacementRule(in.getExecutable().getSignature(), uo.getKind());
                }
            }
        }
        return UnknownRule.UNKNOWN_RULE;
    }
}
