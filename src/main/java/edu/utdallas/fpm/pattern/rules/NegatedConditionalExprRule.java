package edu.utdallas.fpm.pattern.rules;

// if's and while's
public enum NegatedConditionalExprRule implements Rule {
    NEGATED_CONDITIONAL_EXPR_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
