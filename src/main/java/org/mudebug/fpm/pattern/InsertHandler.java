package org.mudebug.fpm.pattern;

import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.Operation;
import org.mudebug.fpm.pattern.rules.Rule;

public class InsertHandler extends Handler {
    public InsertHandler(Handler next) {
        super(next);
    }

    @Override
    protected boolean canHandle(final Operation operation) {
        return operation instanceof InsertOperation;
    }

    @Override
    protected Rule handle(Operation operation) {
        return null;
    }
}
