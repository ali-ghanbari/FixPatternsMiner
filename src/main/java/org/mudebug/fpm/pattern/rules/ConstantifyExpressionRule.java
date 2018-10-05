package org.mudebug.fpm.pattern.rules;

public class ConstantifyExpressionRule implements Rule {
    private final String nullifiedTypeName;
    private final Object constantVal;

    public ConstantifyExpressionRule(String nullifiedTypeName, Object constantVal) {
        this.nullifiedTypeName = nullifiedTypeName;
        this.constantVal = constantVal;
    }
}
