package org.mudebug.fpm.pattern.handler.regexp.cr;

import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.MoveOperation;
import gumtree.spoon.diff.operations.Operation;
import org.mudebug.fpm.pattern.handler.regexp.RegExpHandler;
import org.mudebug.fpm.pattern.handler.regexp.Status;
import org.mudebug.fpm.pattern.rules.ElseBranchRemoved;
import org.mudebug.fpm.pattern.rules.Rule;
import org.mudebug.fpm.pattern.rules.ThenBranchRemoved;
import org.mudebug.fpm.pattern.rules.UnknownRule;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtElement;

import java.util.Iterator;
import java.util.List;

/**
 * warning: the case where one branch is a prefix of the other is not handled
 */
public class IfShortCircuitHandler implements RegExpHandler {
    private final State initState;
    private final AcceptanceState thenRemovedState;
    private final AcceptanceState elseRemovedState;
    private State state;
    private int consumed;

    public IfShortCircuitHandler() {
        this.initState = new InitState();
        this.thenRemovedState = new ThenRemovedState();
        this.elseRemovedState = new ElseRemovedState();
        this.state = this.initState;
        this.consumed = 0;
    }

    private interface State {
        State handle(final Operation operation);
    }

    private class InitState implements State {
        @Override
        public State handle(Operation operation) {
            if (operation instanceof DeleteOperation) {
                final DeleteOperation delOp = (DeleteOperation) operation;
                final CtElement deletedElement = delOp.getSrcNode();
                if (deletedElement instanceof CtIf) {
                    final CtIf ifSt = (CtIf) deletedElement;
                    final CtBlock thenBlock = ifSt.getThenStatement();
                    final CtBlock elseBlock = ifSt.getElseStatement();
                    return new DelState(thenBlock.getStatements(), elseBlock.getStatements());
                }
            }
            return this;
        }
    }

    private class DelState implements State {
        private Iterator<CtStatement> thenIt;
        private Iterator<CtStatement> elseIt;

        public DelState(final List<CtStatement> thenBlock, final List<CtStatement> elseBlock) {
            this.thenIt = thenBlock.stream()
                    .sorted((s1, s2) -> Integer.compare(s1.getPosition().getSourceStart(), s2.getPosition().getSourceStart()))
                    .iterator();
            this.elseIt = elseBlock.stream()
                    .sorted((s1, s2) -> Integer.compare(s1.getPosition().getSourceStart(), s2.getPosition().getSourceStart()))
                    .iterator();
        }

        private State forBlock(final Iterator<CtStatement> blockIt,
                               final CtElement movedElement) {
            if (blockIt == null) {
                return null;
            }
            if (blockIt.hasNext()) {
                final CtStatement statement = blockIt.next();
                movedElement.toString();
                if (statement.equals(movedElement)) {
                    return this;
                } else { // the moved element is not contained in the block
                    return null;
                }
            } else { // something has been moved but we don't have enough element in the block
                return null;
            }
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof MoveOperation) {
                final MoveOperation movOp = (MoveOperation) operation;
                final CtElement movedElement = movOp.getSrcNode();
                final State fromThen = forBlock(this.thenIt, movedElement);
                final State fromElse = forBlock(this.elseIt, movedElement);
                if (fromThen != null) {
                    if (!this.thenIt.hasNext()) {
                        return elseRemovedState;
                    }
                    return this;
                } else {
                    this.thenIt = null;
                    if (fromElse != null) {
                        if (!this.elseIt.hasNext()) {
                            return thenRemovedState;
                        }
                        return this;
                    } else {
                        this.elseIt = null;
                    }
                }
            }
            return initState;
        }
    }

    private interface AcceptanceState extends State {
        Rule getRule();
    }

    private class ThenRemovedState implements AcceptanceState {
        @Override
        public State handle(Operation operation) {
            return initState;
        }

        @Override
        public Rule getRule() {
            return new ThenBranchRemoved();
        }
    }

    private class ElseRemovedState implements AcceptanceState {
        @Override
        public State handle(Operation operation) {
            return initState;
        }

        @Override
        public Rule getRule() {
            return new ElseBranchRemoved();
        }
    }

    private void incConsumed() {
        this.consumed++;
    }

    @Override
    public int getConsumed() {
        return this.consumed;
    }

    @Override
    public void reset() {
        this.consumed = 0;
        this.state = initState;
    }

    @Override
    public Rule getRule() {
        if (this.state instanceof AcceptanceState) {
            return ((AcceptanceState) this.state).getRule();
        }
        return UnknownRule.UNKNOWN_RULE;
    }

    @Override
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
}
