package org.mudebug.fpm.pattern.rules;

public class ArgumentListUpdateRule implements Rule {
    private final String callee;

    public ArgumentListUpdateRule(String callee) {
        this.callee = callee;
    }
}
