package edu.utdallas.fpm.pattern.rules;

public class PreconditionAdditionRule implements Rule {
    private final String paramName;

    public PreconditionAdditionRule(String paramName) {
        this.paramName = paramName;
    }
}
