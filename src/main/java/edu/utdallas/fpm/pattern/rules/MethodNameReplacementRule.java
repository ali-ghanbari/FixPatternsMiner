package edu.utdallas.fpm.pattern.rules;

/**
 * r1.meth1(args1)
 * r2.meth2(args1)
 *
 * where meth1 != meth2 but args1 == args2.
 * r1 and r2 also should be the same.
 */
public class MethodNameReplacementRule implements Rule {
    private final String srcName;
    private final String dstName;

    public MethodNameReplacementRule(String srcName, String dstName) {
        this.srcName = srcName;
        this.dstName = dstName;
    }
}
