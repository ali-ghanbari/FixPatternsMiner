package edu.utdallas.fpm.pattern.rules;

import spoon.reflect.code.BinaryOperatorKind;

public class BinaryOperatorToBiFunctionRule implements Rule {
    private final BinaryOperatorKind deletedBinOpKind;
    private final String functionName;

    public BinaryOperatorToBiFunctionRule(BinaryOperatorKind deletedBinOpKind,
                                          String functionName) {
        this.deletedBinOpKind = deletedBinOpKind;
        this.functionName = functionName;
    }

    public BinaryOperatorKind getDeletedBinaryOperatorKind() {
        return deletedBinOpKind;
    }

    public String getBiFunctionName() {
        return functionName;
    }
}
