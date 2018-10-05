package org.mudebug.fpm.pattern.handler;

import gumtree.spoon.diff.operations.Operation;
import org.mudebug.fpm.pattern.rules.Rule;
import org.mudebug.fpm.pattern.rules.UnknownRule;
import spoon.reflect.declaration.CtElement;

public class DummyOperationHandler extends OperationHandler {
    private static DummyOperationHandler instance = null;

    private DummyOperationHandler() {
        super(null);
    }

    public static synchronized DummyOperationHandler v() {
        if (instance == null) {
            instance = new DummyOperationHandler();
        }
        return instance;
    }

    @Override
    protected boolean canHandlePattern(CtElement e1, CtElement e2) {
        return true;
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        return UnknownRule.UNKNOWN_RULE;
    }

    @Override
    public boolean canHandleOperation(Operation operation) {
        return true;
    }
}
