package org.mudebug.fpm.pattern.handler.regexp;

import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.Operation;
import org.mudebug.fpm.pattern.rules.*;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtElement;

/**
 * warning: this handler might lead to orphan "move" operations
 */
public class FunctionOperatorReplacementHandler extends RegExpHandler {
    public FunctionOperatorReplacementHandler() {
        initState = new InitState();
        this.state = initState;
        this.consumed = 0;
    }

    private class InitState implements State {
        @Override
        public State handle(Operation operation) {
            if (operation instanceof DeleteOperation) {
                final DeleteOperation delOp = (DeleteOperation) operation;
                final CtElement deletedElement = delOp.getSrcNode();
                if (deletedElement instanceof CtInvocation) {
                    final CtInvocation invocation = (CtInvocation) deletedElement;
                    final String methodName = invocation.getExecutable().getSimpleName();
                    final int arity = invocation.getArguments().size();
                    if (arity == 1) {
                        return new DelUnaryInvState(methodName);
                    } else if (arity == 2) {
                        return new DelBinaryInvState(methodName);
                    }
                } else if ((deletedElement instanceof CtBinaryOperator)) {
                    final CtBinaryOperator binOp = (CtBinaryOperator) deletedElement;
                    return new DelBinaryOpState(binOp.getKind());
                } else if (deletedElement instanceof CtUnaryOperator) {
                    final CtUnaryOperator unaryOp = (CtUnaryOperator) deletedElement;
                    return new DelUnaryOpState(unaryOp.getKind());
                }
            }
            return initState;
        }
    }

    private class DelUnaryInvState implements State {
        private final String deletedMethodName;

        public DelUnaryInvState(String deletedMethodName) {
            this.deletedMethodName = deletedMethodName;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final InsertOperation insOp = (InsertOperation) operation;
                final CtElement insertedElement = insOp.getSrcNode();
                if (insertedElement instanceof CtUnaryOperator) {
                    final CtUnaryOperator unaryOperator = (CtUnaryOperator) insertedElement;
                    final UnaryOperatorKind kind = unaryOperator.getKind();
                    return new InsUnaryOpState(this.deletedMethodName, kind);
                }
            }
            return initState;
        }
    }

    private class DelBinaryInvState implements State {
        private final String deletedMethodName;

        public DelBinaryInvState(String deletedMethodName) {
            this.deletedMethodName = deletedMethodName;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final InsertOperation insOp = (InsertOperation) operation;
                final CtElement insertedElement = insOp.getSrcNode();
                if (insertedElement instanceof CtBinaryOperator) {
                    final CtBinaryOperator binaryOperator = (CtBinaryOperator) insertedElement;
                    final BinaryOperatorKind kind = binaryOperator.getKind();
                    return new InsBinOpState(this.deletedMethodName, kind);
                }
            }
            return initState;
        }
    }

    private class InsBinOpState implements AcceptanceState {
        private final String deletedMethodName;
        private final BinaryOperatorKind binOpKind;

        public InsBinOpState(String deletedMethodName, BinaryOperatorKind binOpKind) {
            this.deletedMethodName = deletedMethodName;
            this.binOpKind = binOpKind;
        }

        @Override
        public Rule getRule() {
            return new BiFunctionToBinaryOperatorRule(this.deletedMethodName, this.binOpKind);
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }

    private class InsUnaryOpState implements AcceptanceState {
        private final String deletedMethodName;
        private final UnaryOperatorKind unaryOpKind;

        public InsUnaryOpState(String deletedMethodName, UnaryOperatorKind unaryOpKind) {
            this.deletedMethodName = deletedMethodName;
            this.unaryOpKind = unaryOpKind;
        }

        @Override
        public Rule getRule() {
            return new UnaryFunctionToUnaryOperatorRule(this.deletedMethodName,
                    this.unaryOpKind);
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }

    private class DelUnaryOpState implements State {
        private final UnaryOperatorKind deletedOpKind;

        public DelUnaryOpState(UnaryOperatorKind deletedOpKind) {
            this.deletedOpKind = deletedOpKind;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final InsertOperation insOp = (InsertOperation) operation;
                final CtElement insertedElement = insOp.getSrcNode();
                if (insertedElement instanceof CtInvocation) {
                    final CtInvocation invocation = (CtInvocation) insertedElement;
                    final String methodName = invocation.getExecutable().getSimpleName();
                    final int arity = invocation.getArguments().size();
                    if (arity == 1) {
                        return new InsUnaryFunctionState(this.deletedOpKind, methodName);
                    }
                }
            }
            return initState;
        }
    }

    private class DelBinaryOpState implements State {
        private final BinaryOperatorKind deletedOpKind;

        public DelBinaryOpState(BinaryOperatorKind deletedOpKind) {
            this.deletedOpKind = deletedOpKind;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final InsertOperation insOp = (InsertOperation) operation;
                final CtElement insertedElement = insOp.getSrcNode();
                if (insertedElement instanceof CtInvocation) {
                    final CtInvocation invocation = (CtInvocation) insertedElement;
                    final String methodName = invocation.getExecutable().getSimpleName();
                    final int arity = invocation.getArguments().size();
                    if (arity == 2) {
                        return new InsBiFunctionState(this.deletedOpKind, methodName);
                    }
                }
            }
            return initState;
        }
    }

    private class InsBiFunctionState implements AcceptanceState {
        private final BinaryOperatorKind deletedOpKind;
        private final String insertedMethodName;

        InsBiFunctionState(final BinaryOperatorKind deletedOpKind,
                           final String insertedMethodName) {
            this.deletedOpKind = deletedOpKind;
            this.insertedMethodName = insertedMethodName;
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }

        @Override
        public Rule getRule() {
            return new BinaryOperatorToBiFunctionRule(this.deletedOpKind,
                    this.insertedMethodName);
        }
    }

    private class InsUnaryFunctionState implements AcceptanceState {
        private final UnaryOperatorKind deletedOpKind;
        private final String insertedMethodName;

        public InsUnaryFunctionState(final UnaryOperatorKind deletedOpKind,
                                     final String insertedMethodName) {
            this.deletedOpKind = deletedOpKind;
            this.insertedMethodName = insertedMethodName;
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }

        @Override
        public Rule getRule() {
            return new UnaryOperatorToUnaryFunctionRule(this.deletedOpKind,
                    this.insertedMethodName);
        }
    }
}
