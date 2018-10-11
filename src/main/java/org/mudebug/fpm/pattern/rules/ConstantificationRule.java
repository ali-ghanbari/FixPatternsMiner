package org.mudebug.fpm.pattern.rules;

public class ConstantificationRule implements Rule {
    private final String srcClassName;
    private final String dstLiteral;

    public ConstantificationRule(String srcClassName, String dstLiteral) {
        this.srcClassName = srcClassName;
        this.dstLiteral = dstLiteral;
    }
}
