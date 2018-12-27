package edu.utdallas.fpm.pattern.handler.regexp;

import edu.utdallas.fpm.pattern.rules.*;
import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.Operation;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtTypeReference;

import static edu.utdallas.fpm.commons.Util.sibling;
import static edu.utdallas.fpm.commons.Util.equalsType;

/* please note that in this handler we only take care of "constantification,"
 * meaning that we only check if a complicated literal has turned into a
 * constant (aka literal). other cases, like method -> local or method -> field
 * are to be handled by more specialized handlers (that we already have).
 * please note further that only in case of constantification of new-expressions
 * we only consider default value; other than that we are quite general. */
public class ConstantificationHandler extends RegExpHandler {
    public ConstantificationHandler() {
        initState = new InitState();
        this.state = initState;
        this.consumed = 0;
    }

    // we are going to match DI.
    // while doing so we want the
    // non-trivial deletedExpr deleted
    // and a trivial insertedExp be
    // replaced. we want to make sure
    // that the deleted element and
    // the inserted literal belong
    // to the same parent.
    private class InitState implements State {
        @Override
        public State handle(Operation operation) {
            if (operation instanceof DeleteOperation) {
                final CtElement deletedElement = operation.getSrcNode();
                if (deletedElement instanceof CtExpression) {
                    final CtExpression deletedExpr = (CtExpression) deletedElement;
                    if (deletedExpr instanceof CtInvocation) {
                        /* non-void method call                              */
                        /* an invocation that is literal must be non-void */
                        final CtInvocation deletedInv = (CtInvocation) deletedExpr;
                        return new InvDelState(deletedInv);
                    } else if (deletedExpr instanceof CtConstructorCall
                            || deletedExpr instanceof CtNewClass) {
                        return new CtorCallDelState(deletedExpr);
                    } else if (!isTrivialExp(deletedExpr)) {
                        return new DelExprState(deletedExpr);
                    }
                }
            }
            return initState;
        }

        private boolean isTrivialExp(final CtExpression exp) {
            if (exp instanceof CtUnaryOperator) {
                final CtUnaryOperator unaryOp = (CtUnaryOperator) exp;
                final UnaryOperatorKind kind = unaryOp.getKind();
                switch (kind) {
                    case NEG:
                    case POS:
                    case NOT:
                        final CtExpression operand = unaryOp.getOperand();
                        return operand instanceof CtLiteral;
                }
                return false;
            }
            return exp instanceof CtLiteral;
        }
    }

    private class InvDelState implements State {
        private final CtInvocation deletedInvocation;

        public InvDelState(CtInvocation deletedInvocation) {
            this.deletedInvocation = deletedInvocation;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final CtElement insertedElement = operation.getSrcNode();
                if (insertedElement instanceof CtLiteral) {
                    final CtLiteral insertedLiteral = (CtLiteral) insertedElement;
                    final CtTypeReference returnType = this.deletedInvocation.getType();
                    if (equalsType(returnType, insertedLiteral.getType())) {
                        if (sibling(this.deletedInvocation, insertedLiteral)) {
                            return new InvReplacedState(insertedLiteral);
                        }
                    }
                }
            }
            return initState;
        }
    }

    private class InvReplacedState implements AcceptanceState {
        private final CtLiteral literal;

        public InvReplacedState(final CtLiteral literal) {
            this.literal = literal;
        }

        @Override
        public Rule getRule() {
            return new NonVoidMethCallRemovedRule(this.literal);
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }

    private class CtorCallDelState implements State {
        private final CtExpression deletedCtorCall;

        public CtorCallDelState(CtExpression deletedCtorCall) {
            /* this could be CtConstructorCall or CtNewClass */
            this.deletedCtorCall = deletedCtorCall;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final CtElement insertedElement = operation.getSrcNode();
                if (insertedElement instanceof CtLiteral) {
                    final CtLiteral insertedLiteral = (CtLiteral) insertedElement;
                    if (insertedLiteral.getValue() == null) {
                        if (sibling(this.deletedCtorCall, insertedLiteral)) {
                            return CtorCallReplacedState.CTOR_CALL_REPLACED_STATE;
                        }
                    }
                }
            }
            return initState;
        }
    }

    private enum CtorCallReplacedState implements AcceptanceState {
        CTOR_CALL_REPLACED_STATE;

        @Override
        public Rule getRule() {
            return CtorCallRemovalRule.CTOR_CALL_REMOVAL_RULE;
        }

        @Override
        public State handle(Operation operation) {
            throw new UnsupportedOperationException();
        }
    }

    private class DelExprState implements State {
        private final CtExpression deletedExpr;

        public DelExprState(CtExpression deletedExpr) {
            this.deletedExpr = deletedExpr;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final CtElement insertedElement = operation.getSrcNode();
                if (insertedElement instanceof CtLiteral) {
                    final CtLiteral insertedLiteral = (CtLiteral) insertedElement;
                    final CtTypeReference exprType = this.deletedExpr.getType();
                    if (equalsType(exprType, insertedLiteral.getType())) {
                        if (sibling(this.deletedExpr, insertedLiteral)) {
                            final CtElement parent = insertedLiteral.getParent();
                            if (parent instanceof CtReturn) {
                                return new ReturnedExprReplacedState(insertedLiteral);
                            }
                            return new ExprReplacedState(insertedLiteral);
                        }
                    }
                }
            }
            return initState;
        }
    }

    private class ExprReplacedState implements AcceptanceState {
        private final CtLiteral literal;

        public ExprReplacedState(CtLiteral literal) {
            this.literal = literal;
        }

        @Override
        public Rule getRule() {
            return new ConstantificationRule(this.literal);
        }

        @Override
        public State handle(Operation operation) {
            throw new UnsupportedOperationException();
        }
    }

    private class ReturnedExprReplacedState implements AcceptanceState {
        private final CtLiteral literal;

        public ReturnedExprReplacedState(CtLiteral literal) {
            this.literal = literal;
        }

        @Override
        public Rule getRule() {
            return new ReturnStmtConstantifiedRule(this.literal);
        }

        @Override
        public State handle(Operation operation) {
            throw new UnsupportedOperationException();
        }
    }
}
