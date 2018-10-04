package org.mudebug.fpm.pattern.rules;

public class FieldLocalReplacementRule implements Rule {
    private final String src;
    private final String dst;

    public FieldLocalReplacementRule(final String fieldQualifiedNameOrVarNameSrc,
                                     final String fieldQualifiedNameOrVarNameDst) {
        this.src = fieldQualifiedNameOrVarNameSrc;
        this.dst = fieldQualifiedNameOrVarNameDst;
    }
}
