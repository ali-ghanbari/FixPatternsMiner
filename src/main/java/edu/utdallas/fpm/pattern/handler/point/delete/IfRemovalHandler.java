package edu.utdallas.fpm.pattern.handler.point.delete;

import edu.utdallas.fpm.pattern.handler.OperationHandler;
import edu.utdallas.fpm.pattern.rules.ElseBranchRemovedRule;
import edu.utdallas.fpm.pattern.rules.IfStatementRemovedRule;
import edu.utdallas.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtElement;

import java.util.Objects;

// warning: this handler can lead to orphan M or I if the deleted if statement
// is replaced by something else
// This might lead to imprecision; hopefully it is not significant!
public class IfRemovalHandler extends DeleteHandler {
    protected IfRemovalHandler(OperationHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement e1, CtElement e2) {
        return /*e1 instanceof CtIf ||*/ e1 instanceof CtStatement;
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        if (e1 instanceof CtIf) { /* whole if statement */
            return IfStatementRemovedRule.IF_STATEMENT_REMOVED_RULE;
        }
        /* e1 is definitely a CtStatement */
        final CtElement parent = e1.getParent();
        if (parent instanceof CtIf) { // else is child of an if-statement
            final CtIf parentIf = (CtIf) parent;
            if (Objects.equals(parentIf.getElseStatement(), e1)) {
                return ElseBranchRemovedRule.ELSE_BRANCH_REMOVED_RULE;
            }
        }
        return super.handlePattern(e1, e2);
    }
}
