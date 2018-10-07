package org.mudebug.fpm.pattern.handler.regexp;

import gumtree.spoon.diff.operations.Operation;
import org.mudebug.fpm.pattern.rules.Rule;

public interface RegExpHandler {
    Status handle(final Operation operation);

    Rule getRule();

    void reset();

    int getConsumed();
}
