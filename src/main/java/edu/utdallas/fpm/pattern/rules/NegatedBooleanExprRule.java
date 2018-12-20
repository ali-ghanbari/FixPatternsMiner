package edu.utdallas.fpm.pattern.rules;

// everything other than if's and while's
public enum NegatedBooleanExprRule implements Rule {
    NEGATED_BOOLEAN_EXPR_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
