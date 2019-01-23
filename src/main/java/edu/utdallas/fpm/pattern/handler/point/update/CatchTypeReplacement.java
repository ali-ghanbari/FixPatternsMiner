package edu.utdallas.fpm.pattern.handler.point.update;

import edu.utdallas.fpm.pattern.handler.OperationHandler;
import edu.utdallas.fpm.pattern.rules.CatchTypeChangedRule;
import edu.utdallas.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtCatchVariable;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtTypeReference;

import java.util.Objects;

public class CatchTypeReplacement extends UpdateHandler {
    public CatchTypeReplacement(OperationHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement e1, CtElement e2) {
        return e1 instanceof CtTypeReference
                && e2 instanceof CtTypeReference
                && e1.getParent() instanceof CtCatchVariable
                && e2.getParent() instanceof CtCatchVariable;
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        CtTypeReference tr1 = (CtTypeReference) e1;
        CtTypeReference tr2 = (CtTypeReference) e2;
        if (!Objects.equals(tr1, tr2)) {
            return CatchTypeChangedRule.CATCH_TYPE_CHANGED_RULE;
        }
        return super.handlePattern(e1, e2);
    }
}
