package org.mudebug.fpm.pattern.handler.regexp;

import gumtree.spoon.diff.operations.Operation;
import org.mudebug.fpm.pattern.rules.Rule;
import org.mudebug.fpm.pattern.rules.UnknownRule;

public abstract class RegExpHandler {
    protected State initState;
    protected State state;
    protected int consumed;

    public Status handle(final Operation operation) {
        this.state = this.state.handle(operation);
        if (this.state == initState) { // no progress or rejection
            return Status.REJECTED;
        }
        incConsumed();
        if (this.state instanceof AcceptanceState) {
            return Status.ACCEPTED;
        }
        return Status.CANDIDATE;
    }

    protected void incConsumed() {
        this.consumed++;
    }

    public int getConsumed() {
        return this.consumed;
    }

    public void reset() {
        this.consumed = 0;
        this.state = initState;
    }

    public Rule getRule() {
        if (this.state instanceof AcceptanceState) {
            return ((AcceptanceState) this.state).getRule();
        }
        return UnknownRule.UNKNOWN_RULE;
    }
}
