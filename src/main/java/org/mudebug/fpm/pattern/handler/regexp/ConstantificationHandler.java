package org.mudebug.fpm.pattern.handler.regexp;

import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.Operation;
import org.mudebug.fpm.pattern.rules.ConstantificationRule;
import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.declaration.CtElement;

import static org.mudebug.fpm.commons.Util.sibling;

public class ConstantificationHandler extends RegExpHandler {
    public ConstantificationHandler() {
        initState = new InitState();
        this.state = initState;
        this.consumed = 0;
    }

    // we are going to match DI.
    // while doing so we want the
    // non-trivial deletedExp deleted
    // and a trivial deletedExp be
    // replaced. we want to make sure
    // that the deleted element and
    // the inserted literal belong
    // to the same parent.
    private class InitState implements State {
        @Override
        public State handle(Operation operation) {
            if (operation instanceof DeleteOperation) {
                final DeleteOperation delOp = (DeleteOperation) operation;
                final CtElement deletedElement = delOp.getSrcNode();
                if (deletedElement instanceof CtExpression
                        && !isTrivialExp((CtExpression) deletedElement)) {
                    final CtExpression deletedExp = (CtExpression) deletedElement;
                    return new DelState(deletedExp);
                }
            }
            return initState;
        }

        private boolean isTrivialExp(final CtExpression exp) {
            if (exp instanceof CtUnaryOperator) {
                final CtUnaryOperator unaryOp = (CtUnaryOperator) exp;
                final UnaryOperatorKind kind = unaryOp.getKind();
                if (kind == UnaryOperatorKind.NEG || kind == UnaryOperatorKind.POS) {
                    final CtExpression operand = unaryOp.getOperand();
                    return operand instanceof CtLiteral;
                }
                return false;
            }
            return exp instanceof CtLiteral;
        }
    }

    private class DelState implements State {
        private final CtExpression deletedExp;

        public DelState(CtExpression deletedExp) {
            this.deletedExp = deletedExp;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final InsertOperation insOp = (InsertOperation) operation;
                final CtElement insertedElement = insOp.getSrcNode();
                // inserted element and the deleted expression must be siblings
                if (insertedElement instanceof CtLiteral) {
                    if (sibling(this.deletedExp, insertedElement)) {
                        final CtLiteral insertedLiteral = (CtLiteral) insertedElement;
                        return new Replaced(this.deletedExp,
                                false,
                                insertedLiteral);
                    }
                } else if (insertedElement instanceof CtUnaryOperator) {
                    if (sibling(this.deletedExp, insertedElement)) {
                        final CtUnaryOperator unaryOperator =
                                (CtUnaryOperator) insertedElement;
                        final UnaryOperatorKind kind = unaryOperator.getKind();
                        if (kind == UnaryOperatorKind.NEG
                                || kind == UnaryOperatorKind.POS) {
                            final CtExpression operand = unaryOperator.getOperand();
                            if (operand instanceof CtLiteral) {
                                final CtLiteral insertedLiteral = (CtLiteral) operand;
                                return new Replaced(this.deletedExp,
                                        (kind == UnaryOperatorKind.NEG),
                                        insertedLiteral);
                            }
                        }
                    }
                }
            }
            return initState;
        }
    }

    private class Replaced implements AcceptanceState {
        private final CtExpression deletedExp;
        private final CtLiteral insertedLiteral;
        private final boolean negate;

        Replaced(final CtExpression deletedExp,
                 final boolean negate,
                 final CtLiteral insertedLiteral) {
            this.deletedExp = deletedExp;
            this.insertedLiteral = insertedLiteral;
            this.negate = negate;
        }

        @Override
        public Rule getRule() {
            return new ConstantificationRule(deletedExp.getClass().getName(),
                    (this.negate ? "-" : "") + String.valueOf(insertedLiteral.getValue()));
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }
}
