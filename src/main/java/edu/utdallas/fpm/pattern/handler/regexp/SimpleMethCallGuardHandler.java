package edu.utdallas.fpm.pattern.handler.regexp;

import edu.utdallas.fpm.pattern.rules.SimpleMethCallGuardRule;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.MoveOperation;
import gumtree.spoon.diff.operations.Operation;
import edu.utdallas.fpm.pattern.rules.Rule;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtElement;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static spoon.reflect.code.BinaryOperatorKind.*;

// we call it simple because a method call,
// like m() could return void or anything else
public class SimpleMethCallGuardHandler extends RegExpHandler {
    public SimpleMethCallGuardHandler() {
        initState = new InitState();
        this.state = initState;
        this.consumed = 0;
    }

    // we are going to match IM
    // Insertion of an if statement
    // Moving the void-method call inside that if statement
    private class InitState implements State {
        @Override
        public State handle(Operation operation) {
            if (operation instanceof InsertOperation) {
                final CtElement insertedElement = operation.getSrcNode();
                if (insertedElement instanceof CtIf) {
                    final CtIf insertedIf = (CtIf) insertedElement;
                    final CtExpression guardExp = insertedIf.getCondition();
                    if (guardExp instanceof CtBinaryOperator) {
                        final CtBinaryOperator guardBinOp = ((CtBinaryOperator) guardExp);
                        final CtExpression lhs = guardBinOp.getLeftHandOperand();
                        final CtExpression rhs = guardBinOp.getRightHandOperand();
                        if (lhs instanceof CtLiteral ^ rhs instanceof CtLiteral) {
                            final CtLiteral literal =
                                    (CtLiteral) (lhs instanceof CtLiteral ? lhs : rhs);
                            if (literal.getValue() == null) { // comparison with null
                                final CtExpression base =
                                        lhs instanceof CtLiteral ? rhs : lhs;
                                final BinaryOperatorKind kind = guardBinOp.getKind();
                                final CtStatement thenStatement =
                                        insertedIf.getThenStatement();
                                final CtStatement elseStatement =
                                        insertedIf.getElseStatement();
                                final List<CtStatement> branch;
                                if (kind == NE) {
                                    if (thenStatement instanceof CtBlock) {
                                        branch = ((CtBlock) thenStatement).getStatements();
                                    } else {
                                        branch = Collections.singletonList(thenStatement);
                                    }
                                } else if (kind == EQ) {
                                    if (elseStatement instanceof CtBlock) {
                                        branch = ((CtBlock) elseStatement).getStatements();
                                    } else {
                                        branch = Collections.singletonList(elseStatement);
                                    }
                                } else {
                                    branch = null; // not going to happen in a
                                                   // syntactically correct
                                                   // program
                                }
                                if (branch != null) {
                                    final List<CtInvocation> guardedInvocations =
                                            getGuardedInvocations(branch, base);
                                    if (!guardedInvocations.isEmpty()) {
                                        return new InsIfState(guardedInvocations);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return initState;
        }

        private List<CtInvocation> getGuardedInvocations(final List<CtStatement> branch,
                                                         final CtExpression base) {
            return branch.stream()
                    .filter(CtInvocation.class::isInstance)
                    .map(CtInvocation.class::cast)
                    .filter(i -> i.getTarget().equals(base))
                    .collect(Collectors.toList());
        }
    }

    private class InsIfState implements State {
        private final List<CtInvocation> guardedInvocations;

        public InsIfState(List<CtInvocation> guardedInvocations) {
            this.guardedInvocations = guardedInvocations;
        }

        @Override
        public State handle(Operation operation) {
            if (operation instanceof MoveOperation) {
                final CtElement movedElement = operation.getSrcNode();
                if (movedElement instanceof CtInvocation) {
                    final CtInvocation movedInvocation = (CtInvocation) movedElement;
                    if (guardedInvocations.contains(movedInvocation)) {
                        return new IMState(movedInvocation);
                    }
                }
            }
            return initState;
        }
    }

    private class IMState implements AcceptanceState {
        private final CtInvocation guardedInvocation;

        public IMState(CtInvocation guardedInvocation) {
            this.guardedInvocation = guardedInvocation;
        }

        @Override
        public Rule getRule() {
            return SimpleMethCallGuardRule.SIMPLE_METH_CALL_GUARD_RULE;
        }

        @Override
        public State handle(Operation operation) {
            return initState;
        }
    }
}
