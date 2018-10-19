package org.mudebug.fpm.pattern.rules;

public class PreconditionAdditionRule implements Rule {
    private final String paramName;

    public PreconditionAdditionRule(String paramName) {
        this.paramName = paramName;
    }
}
