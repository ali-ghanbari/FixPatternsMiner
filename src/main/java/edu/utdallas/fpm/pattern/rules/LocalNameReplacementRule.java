package edu.utdallas.fpm.pattern.rules;

public class LocalNameReplacementRule implements Rule {
    private final String srcName;
    private final String dstName;

    public LocalNameReplacementRule(String srcName, String dstName) {
        this.srcName = srcName;
        this.dstName = dstName;
    }
}
