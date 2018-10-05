package org.mudebug.fpm.pattern.rules;

public class NakedInvocationRule implements Rule {
    private final String signature;
    private final int index;

    public NakedInvocationRule(String signature, int index) {
        this.signature = signature;
        this.index = index;
    }
}
