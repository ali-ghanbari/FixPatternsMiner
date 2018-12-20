package edu.utdallas.fpm.pattern.rules;

import spoon.reflect.code.UnaryOperatorKind;

public class UnaryOperatorToUnaryFunctionRule implements Rule {
    private final UnaryOperatorKind deletedUnaryOpKind;
    private final String functionName;

    public UnaryOperatorToUnaryFunctionRule(UnaryOperatorKind deletedUnaryOpKind,
                                            String functionName) {
        this.deletedUnaryOpKind = deletedUnaryOpKind;
        this.functionName = functionName;
    }

    public UnaryOperatorKind getDeletedUnaryOpKind() {
        return deletedUnaryOpKind;
    }

    public String getFunctionName() {
        return functionName;
    }

    @Override
    public String getId() {
        return String.format("%s (%s -> %s)",
                this.getClass().getSimpleName(),
                this.deletedUnaryOpKind.name(),
                this.functionName);
    }
}
