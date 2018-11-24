package edu.utdallas.fpm.pattern.handler.point.delete;

import edu.utdallas.fpm.pattern.rules.MemberVarAssignmentDeletionRule;
import edu.utdallas.fpm.pattern.handler.OperationHandler;
import edu.utdallas.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;

// warning: this handler might lead orphaned M or I in case the deletion
// is followed by some M or I.
// this potentially leads to imprecision; hopefully it is not that much!
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
