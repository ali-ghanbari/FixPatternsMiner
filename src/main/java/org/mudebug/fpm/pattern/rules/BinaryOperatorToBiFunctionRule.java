package org.mudebug.fpm.pattern.rules;

import spoon.reflect.code.BinaryOperatorKind;

public class BinaryOperatorToBiFunctionRule implements Rule {
    private final BinaryOperatorKind deletedBinOpKind;
    private final String functionName;

    public BinaryOperatorToBiFunctionRule(BinaryOperatorKind deletedBinOpKind,
                                          String functionName) {
        this.deletedBinOpKind = deletedBinOpKind;
        this.functionName = functionName;
    }
}
