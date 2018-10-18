package org.mudebug.fpm.pattern.handler.point.delete;

import org.mudebug.fpm.pattern.handler.OperationHandler;
import org.mudebug.fpm.pattern.rules.MemberVarAssignmentDeletionRule;
import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;

public class FieldInitRemovalHandler extends DeleteHandler {
    public FieldInitRemovalHandler(OperationHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement e1, CtElement e2) {
        return e1 instanceof CtAssignment;
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        CtElement parent = e1.getParent();
        while (parent instanceof CtStatement || parent instanceof CtExpression) {
            parent = parent.getParent();
        }
        if (parent instanceof CtConstructor) {
            final CtAssignment assignment = (CtAssignment) e1;
            final CtExpression assigned = assignment.getAssigned();
            if (assigned instanceof CtFieldWrite) {
                final CtFieldWrite fieldWrite = (CtFieldWrite) assigned;
                final String fieldName = fieldWrite.getVariable().getSimpleName();
                return new MemberVarAssignmentDeletionRule(fieldName);
            }
        }
        return super.handlePattern(e1, e2);
    }
}
