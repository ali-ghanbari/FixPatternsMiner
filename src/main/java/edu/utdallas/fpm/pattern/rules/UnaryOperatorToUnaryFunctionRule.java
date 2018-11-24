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
}
