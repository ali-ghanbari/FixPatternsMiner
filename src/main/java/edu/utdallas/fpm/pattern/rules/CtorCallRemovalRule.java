package edu.utdallas.fpm.pattern.rules;

import spoon.reflect.code.CtLiteral;

import static edu.utdallas.fpm.pattern.rules.commons.Util.renderLiteral;

/* java classes for literals, like String and Numbers, are all final,
 * so the only plausible constant in place of a new expression would
 * be null */
public enum  CtorCallRemovalRule implements Rule {
    CTOR_CALL_REMOVAL_RULE;

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
