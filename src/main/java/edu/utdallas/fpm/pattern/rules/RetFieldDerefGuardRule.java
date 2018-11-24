package edu.utdallas.fpm.pattern.rules;

public class RetFieldDerefGuardRule implements Rule {
    private final String fieldName;

    public RetFieldDerefGuardRule(String fieldName) {
        this.fieldName = fieldName;
    }
}
