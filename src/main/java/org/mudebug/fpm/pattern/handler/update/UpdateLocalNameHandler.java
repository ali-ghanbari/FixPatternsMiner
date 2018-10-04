package org.mudebug.fpm.pattern.handler.update;

import org.mudebug.fpm.pattern.rules.LocalNameReplacementRule;
import org.mudebug.fpm.pattern.rules.Rule;
import org.mudebug.fpm.pattern.rules.UnknownRule;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.declaration.CtElement;

public class UpdateLocalNameHandler extends UpdateHandler {
    protected UpdateLocalNameHandler(UpdateHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement e1, CtElement e2) {
        return e1 instanceof CtVariableAccess && e2 instanceof CtVariableAccess;
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        final CtVariableAccess va1 = (CtVariableAccess) e1;
        final CtVariableAccess va2 = (CtVariableAccess) e2;
        if (va1.getType().equals(va2.getType())) {
            final String srcName = va1.getVariable().getSimpleName();
            final String dstName = va2.getVariable().getSimpleName();
            return new LocalNameReplacementRule(srcName, dstName);
        }
        return UnknownRule.UNKNOWN_RULE;
    }
}
