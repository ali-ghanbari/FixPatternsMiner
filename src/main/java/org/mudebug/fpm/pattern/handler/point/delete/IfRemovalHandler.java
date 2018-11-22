package org.mudebug.fpm.pattern.handler.point.delete;

import org.mudebug.fpm.pattern.handler.OperationHandler;
import org.mudebug.fpm.pattern.rules.*;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtIf;
import spoon.reflect.declaration.CtElement;

// warning: this handler can lead to orphan M or I if the deleted if statement
// is replaced by something else
// This might lead to imprecision; hopefully it is not significant!
public class IfRemovalHandler extends DeleteHandler {
    protected IfRemovalHandler(OperationHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement e1, CtElement e2) {
        return e1 instanceof CtIf || e1 instanceof CtBlock;
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        if (e1 instanceof CtBlock) { // probably else block
            final CtElement parent = e1.getParent();
            if (parent instanceof CtIf) {
                return ElseBranchRemovedRule.ELSE_BRANCH_REMOVED_RULE;
            }
            return super.handlePattern(e1, e2);
        }
        /* whole if statement */
        final CtIf ifStmt = (CtIf) e1;
        if (ifStmt.getElseStatement() != null) {
            return IfStatementRemovedRule.IF_STATEMENT_REMOVED_RULE;
        }
        return ThenBranchRemovedRule.THEN_BRANCH_REMOVED_RULE;
    }
}
