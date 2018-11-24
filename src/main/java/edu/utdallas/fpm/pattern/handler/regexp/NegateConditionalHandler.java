package edu.utdallas.fpm.pattern.handler.regexp;

import edu.utdallas.fpm.commons.Util;
import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.MoveOperation;
import gumtree.spoon.diff.operations.Operation;
import edu.utdallas.fpm.pattern.rules.NegatedBooleanExprRule;
import edu.utdallas.fpm.pattern.rules.NegatedConditionalExprRule;
import edu.utdallas.fpm.pattern.rules.Rule;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtTypeReference;

public class NegateConditionalHandler extends RegExpHandler {
    public NegateConditionalHandler() {
        initState = new InitState();
        this.state = initState;
        this.consumed = 0;
    }

    // we are going to match either IM, DI, or DM.
    // therefore, there will be two outgoing
    // edges from this node:
    //  - Deletion of a boolean expression,
    //  - Insertion of a negated boolean
    //    expression.
    private class InitState implements State {
        @Override
        public State handle(Operation operation) {
            if (operation instanceof DeleteOperation) {
                final CtElement deletedElement = operation.getSrcNode();
                final CtElement parentElement  = deletedElement.getParent();
                if (deletedElement instanceof CtExpression) {
                    final CtExpression deletedExp = (CtExpression) deletedElement;
                    if (deletedExp instanceof CtUnaryOperator) {
                        final CtUnaryOperator deletedUnaryOp =
                                (CtUnaryOperator) deletedExp;
                        final UnaryOperatorKind opKind = deletedUnaryOp.getKind();
                        if (opKind == UnaryOperatorKind.NOT) {
                            final CtExpression negatedExp = deletedUnaryOp.getOperand();
                            return new DelInsBoolExpState(parentElement, negatedExp);
                        }
                    } else {
                        final CtTypeReference typeRef = deletedExp.getType();
                        if (typeRef != null) {
                            final String expTypeName = typeRef.getSimpleName();
                            if (isBoolean(expTypeName)) {
                                return new DelInsBoolExpState(parentElement, deletedExp);
                            }
                        }
                    }
                }
            } else if (operation instanceof InsertOperation) {
                final CtElement insertedElement = operation.getSrcNode();
                final CtElement parentElement  = insertedElement.getParent();
                if (insertedElement instanceof CtUnaryOperator) {
                    final CtUnaryOperator insertedUnaryOp =
                            (CtUnaryOperator) insertedElement;
                    final UnaryOperatorKind unaryOpKind = insertedUnaryOp.getKind();
                    if (unaryOpKind == UnaryOperatorKind.NOT) {
                        final CtExpression negatedExp = insertedUnaryOp.getOperand();
                        return new DelInsBoolExpState(parentElement, negatedExp);
                    }
                }
            }
            return this;
        }

        private boolean isBoolean (final String typeName) {
            return typeName.matches("(boolean)|(Boolean)");
        }
    }

    private class DelInsBoolExpState implements State {
        private final CtElement parentElement;
        private final CtExpression boolExp;

        public DelInsBoolExpState(CtElement parentElement, CtExpression boolExp) {
            this.parentElement = parentElement;
            this.boolExp = boolExp;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final CtElement insertedElement = operation.getSrcNode();
                if (insertedElement instanceof CtUnaryOperator) {
                    final CtUnaryOperator insertedUnaryOp =
                            (CtUnaryOperator) insertedElement;
                    final UnaryOperatorKind opKind = insertedUnaryOp.getKind();
                    if (opKind == UnaryOperatorKind.NOT) {
                        if (Util.textEquals(insertedUnaryOp.getOperand(), this.boolExp)) {
                            return new DIState(this.parentElement);
                        }
                    }
                } else if (insertedElement instanceof CtExpression) {
                    final CtExpression insertedExp = (CtExpression) insertedElement;
                    if (Util.textEquals(insertedExp, this.boolExp)) {
                        return new DM_IM_State(this.parentElement);
                    }
                }
            } else if (operation instanceof MoveOperation) {
                final CtElement movedElement = operation.getSrcNode();
                if (movedElement instanceof CtExpression) {
                    final CtExpression movedExp = (CtExpression) movedElement;
                    if (Util.textEquals(this.boolExp, movedExp)) {
                        return new DM_IM_State(this.parentElement);
                    }
                }
            }
            return initState;
        }
    }

    private class DM_IM_State extends NegateConditionalAcceptanceState {
        public DM_IM_State(CtElement parentElement) {
            super(parentElement);
        }
    }

    private class DIState extends NegateConditionalAcceptanceState {
        public DIState(CtElement parentElement) {
            super(parentElement);
        }
    }

    private abstract class NegateConditionalAcceptanceState implements AcceptanceState {
        protected final CtElement parentElement;

        public NegateConditionalAcceptanceState(CtElement parentElement) {
            this.parentElement = parentElement;
        }

        @Override
        public Rule getRule() {
            if (parentElement instanceof CtIf
                    || parentElement instanceof CtWhile
                    || parentElement instanceof CtConditional
                    || parentElement instanceof CtDo) {
                return new NegatedConditionalExprRule();
            }
            return new NegatedBooleanExprRule();
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }
}
