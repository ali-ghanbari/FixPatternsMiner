package org.mudebug.fpm.pattern.rules;

/**
 * r1.meth1(args1)
 * r2.meth2(args1)
 *
 * where meth1 == meth2 but args1 != args2.
 * r1 and r2 are the same type.
 */
public class ArgumentListRule implements Rule {
}
