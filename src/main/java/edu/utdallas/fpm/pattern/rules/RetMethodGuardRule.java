package edu.utdallas.fpm.pattern.rules;

public class RetMethodGuardRule implements Rule {
    private final String methodName;

    public RetMethodGuardRule(String methodName) {
        this.methodName = methodName;
    }
}
