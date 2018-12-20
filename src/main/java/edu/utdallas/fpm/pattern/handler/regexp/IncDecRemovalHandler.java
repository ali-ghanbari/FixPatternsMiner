package edu.utdallas.fpm.pattern.handler.regexp;

import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.Operation;
import edu.utdallas.fpm.pattern.rules.DecrementsRemovalRule;
import edu.utdallas.fpm.pattern.rules.IncrementsRemovalRule;
import edu.utdallas.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.declaration.CtElement;

import static edu.utdallas.fpm.commons.Util.textEquals;

// warning: in some cases, this handler might leave an orphan M operation
// e.g. (l.id(l).field)++ ---> l.id(l).field
// is DIM, where D deletes (l.id(l).field)++, I inserts l.id(l).field,
// and M moved l.id(l)
public class IncDecRemovalHandler extends RegExpHandler {
    public IncDecRemovalHandler() {
        initState = new InitState();
        this.state = initState;
        this.consumed = 0;
    }

    // DI
    // Deletion of exp++, ++exp, exp--, or --exp
    // Insertion of exp
    private class InitState implements State {
        @Override
        public State handle(Operation operation) {
            if (operation instanceof DeleteOperation) {
                final CtElement deletedElement = operation.getSrcNode();
                if (deletedElement instanceof CtUnaryOperator) {
                    final CtUnaryOperator deletedUnOp = (CtUnaryOperator) deletedElement;
                    if (isIncDec(deletedUnOp)) {
                        return new DeletedIncDecState(deletedUnOp);
                    }
                }
            }
            return initState;
        }

        private boolean isIncDec(final CtUnaryOperator unaryOperator) {
            final UnaryOperatorKind kind = unaryOperator.getKind();
            return  kind == UnaryOperatorKind.PREINC
                    || kind == UnaryOperatorKind.POSTINC
                    || kind == UnaryOperatorKind.PREDEC
                    || kind == UnaryOperatorKind.POSTDEC;
        }
    }

    private class DeletedIncDecState implements State {
        private final CtUnaryOperator deletedUnOp;

        public DeletedIncDecState(CtUnaryOperator deletedUnOp) {
            this.deletedUnOp = deletedUnOp;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final CtElement insertedElement = operation.getSrcNode();
                if (insertedElement instanceof CtExpression) {
                    final CtExpression insertedExpr = (CtExpression) insertedElement;
                    if (textEquals(insertedExpr, this.deletedUnOp.getOperand())) {
                        final UnaryOperatorKind kind = this.deletedUnOp.getKind();
                        return new DIState(kind);
                    }
                }
            }
            return initState;
        }
    }

    private class DIState implements AcceptanceState {
        private final UnaryOperatorKind kind;

        public DIState(UnaryOperatorKind kind) {
            this.kind = kind;
        }

        @Override
        public Rule getRule() {
            if (this.kind == UnaryOperatorKind.PREINC
                    || this.kind == UnaryOperatorKind.POSTINC) {
                return IncrementsRemovalRule.INCREMENTS_REMOVAL_RULE;
            }
            return DecrementsRemovalRule.DECREMENTS_REMOVAL_RULE;
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }
}
