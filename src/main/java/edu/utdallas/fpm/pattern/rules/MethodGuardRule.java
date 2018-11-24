package edu.utdallas.fpm.pattern.rules;

public class MethodGuardRule implements Rule {
    private final String methodName;

    public MethodGuardRule(String methodName) {
        this.methodName = methodName;
    }
}
