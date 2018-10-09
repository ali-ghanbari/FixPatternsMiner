package org.mudebug.fpm.pattern.handler.regexp;

import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.MoveOperation;
import gumtree.spoon.diff.operations.Operation;
import org.mudebug.fpm.pattern.rules.*;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtElement;

import java.util.*;

public class IfShortCircuitHandler extends RegExpHandler {
    private final AcceptanceState thenRemovedState;
    private final AcceptanceState elseRemovedState;
    private final AcceptanceState ifRemovedState;

    public IfShortCircuitHandler() {
        this.initState = new InitState();
        this.thenRemovedState = new ThenRemovedState();
        this.elseRemovedState = new ElseRemovedState();
        this.ifRemovedState = new IfRemovedState();
        this.state = this.initState;
        this.consumed = 0;
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
                    if (thenBlock == null && elseBlock == null) { // if(*);
                        return new IfRemovedState();
                    }
                    final List<CtStatement> thenBlockList =
                            thenBlock == null ? null : thenBlock.getStatements();
                    final List<CtStatement> elseBlockList =
                            elseBlock == null ? null : elseBlock.getStatements();
                    if (thenBlockList == null || elseBlock == null) {
                        return new DelIfState(
                                thenBlockList == null ? elseBlockList : thenBlockList
                        );
                    }
                    return new DelBranchState(thenBlockList, elseBlockList);
                }
            }
            return this;
        }
    }

    private abstract class DelState implements State {
        protected State forBlock(final Iterator<CtStatement> blockIt,
                               final CtElement movedElement) {
            if (blockIt == null) {
                return null;
            }
            if (blockIt.hasNext()) {
                final CtStatement statement = blockIt.next();
                //movedElement.toString(); this is due to a bug in GumTree version 1.5
                if (statement.equals(movedElement)) {
                    return this;
                } else { // the moved element is not contained in the block
                    return null;
                }
            } else { // something has been moved but we don't have enough element in the block
                return null;
            }
        }
    }

    private class DelIfState extends DelState {
        private Iterator<CtStatement> blockIt;

        public DelIfState(final List<CtStatement> block) {
            this.blockIt = block.stream()
                    .sorted(Comparator.comparingInt(s -> s.getPosition().getSourceStart()))
                    .iterator();
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof MoveOperation) {
                final MoveOperation movOp = (MoveOperation) operation;
                final CtElement movedElement = movOp.getSrcNode();
                final State formBlock = forBlock(this.blockIt, movedElement);
                if (formBlock != null) {
                    if (!this.blockIt.hasNext()) {
                        return ifRemovedState;
                    }
                    return this;
                }
            }
            return initState;
        }
    }

    private class DelBranchState extends DelState {
        private Iterator<CtStatement> thenIt;
        private Iterator<CtStatement> elseIt;

        public DelBranchState(final List<CtStatement> thenBlock, final List<CtStatement> elseBlock) {
            this.thenIt = thenBlock.stream()
                    .sorted(Comparator.comparingInt(s -> s.getPosition().getSourceStart()))
                    .iterator();
            this.elseIt = elseBlock.stream()
                    .sorted(Comparator.comparingInt(s -> s.getPosition().getSourceStart()))
                    .iterator();
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof MoveOperation) {
                final MoveOperation movOp = (MoveOperation) operation;
                final CtElement movedElement = movOp.getSrcNode();
                final State fromThen = forBlock(this.thenIt, movedElement);
                final State fromElse = forBlock(this.elseIt, movedElement);
                if (fromThen != null && fromElse != null) {
                    // this is when one list is a prefix of the other.
                    // in such a case, one of the iterators reaches
                    // temporarily reaches to its end while the other
                    // is not done yet. so we stay in whatever state
                    // we are. in the next iteration, either of the
                    // iterators (or both of them) will be nullified.
                    // there is one caveat that the if condition has
                    // exactly the same sequence of instruction in
                    // each of its branches. in such a case, we can
                    // easily say the "else" branch is removed.
                    if (!this.thenIt.hasNext() && !this.elseIt.hasNext()) {
                        return elseRemovedState;
                    }
                    return this;
                } else {
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
            }
            return initState;
        }
    }

    private class ThenRemovedState implements AcceptanceState {
        @Override
        public State handle(Operation operation) {
            return initState;
        }

        @Override
        public Rule getRule() {
            return new ThenBranchRemovedRule();
        }
    }

    private class ElseRemovedState implements AcceptanceState {
        @Override
        public State handle(Operation operation) {
            return initState;
        }

        @Override
        public Rule getRule() {
            return new ElseBranchRemovedRule();
        }
    }

    private class IfRemovedState implements AcceptanceState {

        @Override
        public Rule getRule() {
            return IfStatementRemovedRule.IF_STATEMENT_REMOVED_RULE;
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }
}
