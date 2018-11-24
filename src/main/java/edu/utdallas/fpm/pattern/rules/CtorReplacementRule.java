package edu.utdallas.fpm.pattern.rules;

public class CtorReplacementRule implements Rule {
    private final String srcTypeName;
    private final String dstTypeName;

    public CtorReplacementRule(String srcTypeName, String dstTypeName) {
        this.srcTypeName = srcTypeName;
        this.dstTypeName = dstTypeName;
    }
}
