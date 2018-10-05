package org.mudebug.fpm.pattern.handler.update;

import org.mudebug.fpm.pattern.handler.OperationHandler;
import org.mudebug.fpm.pattern.rules.NakedInvocationRule;
import org.mudebug.fpm.pattern.rules.Rule;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtExecutableReference;

import java.util.ArrayList;
import java.util.List;

public class UpdateNakedMethCallHandler extends UpdateHandler {
    public UpdateNakedMethCallHandler(OperationHandler next) {
        super(next);
    }

    @Override
    protected boolean canHandlePattern(CtElement e1, CtElement e2) {
        if ((e1 instanceof CtInvocation && e2 instanceof CtExpression)
                || (e2 instanceof CtInvocation && e1 instanceof CtExpression)) {
            final CtInvocation in = getInvocation(e1, e2);
            final CtExpression ex = in == e1 ? (CtExpression) e2 : (CtExpression) e1;
            final List<CtExpression> args = new ArrayList<>();
            args.add(in.getTarget());
            args.addAll(in.getArguments());
            return args.indexOf(ex) >= 0;
        }
        return false;
    }

    private CtInvocation getInvocation(CtElement e1, CtElement e2) {
        return (e1 instanceof CtInvocation) ? (CtInvocation) e1 : (CtInvocation) e2;
    }

    @Override
    protected Rule handlePattern(CtElement e1, CtElement e2) {
        final CtInvocation in = getInvocation(e1, e2);
        final CtExpression ex = in == e1 ? (CtExpression) e2 : (CtExpression) e1;
        final List<CtExpression> args = new ArrayList<>();
        args.add(in.getTarget());
        args.addAll(in.getArguments());
        final int index = args.indexOf(ex);
        if (index >= 0) {
            final CtExecutableReference method = in.getExecutable();
            return new NakedInvocationRule(method.getSignature(), index);
        }
        return super.handlePattern(e1, e2);
    }
}
