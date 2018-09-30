package org.mudebug.fpm.pattern;

import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.Operation;
import org.mudebug.fpm.pattern.rules.Rule;

public class DeleteHandler extends Handler {
    public DeleteHandler(Handler next) {
        super(next);
    }

    @Override
    protected boolean canHandle(final Operation operation) {
        return operation instanceof DeleteOperation;
    }

    @Override
    protected Rule handle(Operation operation) {
        return null;
    }
}
