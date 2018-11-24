package edu.utdallas.fpm.pattern.rules;

public class MethodToLocalReplacementRule implements Rule {
    private final String methodName;
    private final String localName;

    public MethodToLocalReplacementRule(String methodName, String localName) {
        this.methodName = methodName;
        this.localName = localName;
    }
}
