package org.mudebug.fpm.pattern.handler.regexp;

import gumtree.spoon.diff.operations.Operation;

public interface State {
    State handle(Operation operation);
}
