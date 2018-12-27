package edu.utdallas.fpm.pattern.handler.point.insert;

import edu.utdallas.fpm.pattern.rules.PreconditionAdditionRule;
import edu.utdallas.fpm.pattern.handler.OperationHandler;
import edu.utdallas.fpm.pattern.rules.Rule;
import edu.utdallas.fpm.pattern.rules.UsagePreference;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static edu.utdallas.fpm.commons.Util.getReturnStmt;
import static edu.utdallas.fpm.commons.Util.getExecutableContainer;

public class PreconditionAdditionHandler extends InsertHandler {
    public PreconditionAdditionHandler(OperationHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement e1, CtElement e2) {
        return e1 instanceof CtIf;
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        final CtIf ifStmt = (CtIf) e1;
        BinaryOperatorKind expectedBinOp = null;
        UsagePreference usagePreference = getReturnStmt(ifStmt.getThenStatement());
        if (usagePreference != null) {
            expectedBinOp = BinaryOperatorKind.EQ;
        } else {
            usagePreference = getReturnStmt(ifStmt.getElseStatement());
            if (usagePreference != null) {
                expectedBinOp = BinaryOperatorKind.NE;
            }
        }
        if (expectedBinOp != null) {
            final Set<String> paramNames = new HashSet<>();
            Iterator paramsIt = null;
            final CtElement container = getExecutableContainer(ifStmt);
            if (container instanceof CtMethod) {
                final CtMethod method = (CtMethod) container;
                paramsIt = method.getParameters().iterator();

            } else if (container instanceof CtConstructor) {
                final CtConstructor ctor = (CtConstructor) container;
                paramsIt = ctor.getParameters().iterator();
            } else if (container instanceof CtLambda) {
                final CtLambda lambda = (CtLambda) container;
                paramsIt = lambda.getParameters().iterator();
            }
            if (paramsIt != null) {
                while(paramsIt.hasNext()) {
                    final CtParameter parameter = (CtParameter) paramsIt.next();
                    paramNames.add(parameter.getSimpleName());
                }
            }
            if (!paramNames.isEmpty()) {
                Iterator<CtElement> it = ifStmt.getCondition().descendantIterator();
                while (it.hasNext()) {
                    final CtElement element = it.next();
                    if (element instanceof CtBinaryOperator) {
                        final CtBinaryOperator binOp = (CtBinaryOperator) element;
                        final BinaryOperatorKind binOpKind = binOp.getKind();
                        if (binOpKind == expectedBinOp) {
                            final CtExpression lho = binOp.getLeftHandOperand();
                            final CtExpression rho = binOp.getRightHandOperand();
                            if (lho instanceof CtLiteral ^ rho instanceof CtLiteral) {
                                final CtLiteral literal =
                                        (CtLiteral) (lho instanceof CtLiteral ? lho : rho);
                                if (literal.getValue() == null) {
                                    if (lho instanceof CtVariableAccess
                                            ^ rho instanceof CtVariableAccess) {
                                        final CtVariableAccess varAccess =
                                                (CtVariableAccess) (lho instanceof CtVariableAccess ? lho : rho);
                                        final String varName = varAccess.getVariable().getSimpleName();
                                        if (paramNames.contains(varName)) {
                                            return new PreconditionAdditionRule(usagePreference);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return super.handlePattern(e1, e2);
    }
}
