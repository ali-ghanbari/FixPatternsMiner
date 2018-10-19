package org.mudebug.fpm.pattern.handler.point.insert;

import org.mudebug.fpm.commons.Util;
import org.mudebug.fpm.pattern.handler.OperationHandler;
import org.mudebug.fpm.pattern.handler.util.EitherFieldOrMethod;
import org.mudebug.fpm.pattern.handler.util.FieldAccess;
import org.mudebug.fpm.pattern.handler.util.MethodInvocation;
import org.mudebug.fpm.pattern.rules.RetFieldDerefGuardRule;
import org.mudebug.fpm.pattern.rules.RetMethodGuardRule;
import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtElement;

import java.util.Iterator;

public class RetFieldMethDerefGuardHandler extends InsertHandler {
    public RetFieldMethDerefGuardHandler(OperationHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement e1, CtElement e2) {
        return e1 instanceof CtIf;
    }

    private CtExpression getCheckedExp (final CtIf ifStmt) {
        final CtExpression cond = ifStmt.getCondition();
        if (cond instanceof CtBinaryOperator) {
            final CtBinaryOperator binOp = (CtBinaryOperator) cond;
            final BinaryOperatorKind binOpKind = binOp.getKind();
            if (binOpKind == BinaryOperatorKind.EQ
                    || binOpKind == BinaryOperatorKind.NE) {
                final CtExpression lhs = binOp.getLeftHandOperand();
                final CtExpression rhs = binOp.getRightHandOperand();
                if (lhs instanceof CtLiteral ^ rhs instanceof CtLiteral) {
                    final CtLiteral literal =
                            (CtLiteral) (lhs instanceof CtLiteral ? lhs : rhs);
                    final CtExpression checkedExp =
                            lhs instanceof CtLiteral ? rhs : lhs;
                    if (literal.getValue() == null) {
                        return checkedExp;
                    }
                }
            }
        }
        return null;
    }

    private EitherFieldOrMethod getDeferenceStmt(final CtElement stmtBlock,
                                                 final CtExpression target) {
        if (!(stmtBlock instanceof CtStatement)) {
            return null;
        }
        final Iterator<CtElement> it = stmtBlock.descendantIterator();
        while (it.hasNext()) {
            final CtElement element = it.next();
            if (element instanceof CtFieldAccess) {
                final CtFieldAccess fieldAccess = (CtFieldAccess) element;
                if (target.equals(fieldAccess.getTarget())) {
                    return new FieldAccess(fieldAccess);
                }
            } else if (element instanceof CtInvocation) {
                final CtInvocation invocation = (CtInvocation) element;
                if (target.equals(invocation.getTarget())) {
                    return new MethodInvocation(invocation);
                }
            }
        }
        return null;
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        final CtIf ifStmt = (CtIf) e1;
        final CtExpression checkedExp = getCheckedExp(ifStmt);
        EitherFieldOrMethod fieldOrMethod = null;
        if (checkedExp != null) {
            if (Util.containsReturn(ifStmt.getThenStatement())) {
                fieldOrMethod = getDeferenceStmt(ifStmt.getParent(), checkedExp);
                if (fieldOrMethod == null) {
                    fieldOrMethod = getDeferenceStmt(ifStmt.getElseStatement(), checkedExp);
                }
            } else if (Util.containsReturn(ifStmt.getElseStatement())) {
                fieldOrMethod = getDeferenceStmt(ifStmt.getParent(), checkedExp);
                if (fieldOrMethod == null) {
                    fieldOrMethod = getDeferenceStmt(ifStmt.getThenStatement(), checkedExp);
                }
            }
        }
        if (fieldOrMethod != null) {
            if (fieldOrMethod instanceof FieldAccess) {
                final FieldAccess fieldAccess = (FieldAccess) fieldOrMethod;
                return new RetFieldDerefGuardRule(fieldAccess.getFieldAccess()
                        .getVariable()
                        .getQualifiedName());
            } else {
                final MethodInvocation invocation = (MethodInvocation) fieldOrMethod;
                return new RetMethodGuardRule(invocation.getMethodInvocation()
                        .getExecutable()
                        .getSignature());
            }
        }
        return super.handlePattern(e1, e2);
    }
}
