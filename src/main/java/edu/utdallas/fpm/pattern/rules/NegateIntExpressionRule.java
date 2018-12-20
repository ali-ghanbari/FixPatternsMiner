package edu.utdallas.fpm.pattern.rules;

public enum NegateIntExpressionRule implements Rule {
    NEGATE_INT_EXPRESSION_RULE;
    
    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
