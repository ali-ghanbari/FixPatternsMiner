package org.mudebug.fpm.main;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.Operation;
import org.mudebug.fpm.commons.FileListParser;
import org.mudebug.fpm.commons.FilePairVisitor;
import org.mudebug.fpm.pattern.handler.OperationHandler;
import org.mudebug.fpm.pattern.handler.point.delete.DeleteHandler;
import org.mudebug.fpm.pattern.handler.point.insert.InsertHandler;
import org.mudebug.fpm.pattern.handler.point.update.UpdateHandler;
import org.mudebug.fpm.pattern.handler.regexp.*;

import static java.lang.System.out;

public final class Main implements FilePairVisitor {
    private final RegExpHandler[] regExpHandlers;
    private final OperationHandler[] pointHandlers;

    private Main() {
        this.pointHandlers = new OperationHandler[] {
                DeleteHandler.createHandlerChain(),
                InsertHandler.createHandlerChain(),
                UpdateHandler.createHandlerChain()
        };
        this.regExpHandlers = new RegExpHandler[] {
                new IfShortCircuitHandler(),
                new DecomposedMethodCallHandler(),
                new DecomposeBinaryOperatorHandler(),
                new ConstantificationHandler(),
                new FunctionOperatorReplacementHandler(),
                new FieldLocalReplacementHandler()
        };
    }

    public static void main(String[] args) {
        final FileListParser parser;
        if (args.length > 1) {
            out.println("fatal: too many arguments");
            return;
        } else if (args.length < 1) {
            parser = new FileListParser();
        } else {
            parser = new FileListParser(new File(args[0]));
        }
        final Main visitor = new Main();
        parser.parse(visitor, false);
    }

    @Override
    public void visit(final File buggy, final File fixed) {
        out.printf("Diffing (%s):%n\t%s%n\t%s%n", buggy.getParent(), buggy.getName(), fixed.getName());
        final AstComparator ac = new AstComparator();
        try {
            final Diff diff = ac.compare(buggy, fixed);
            final List<Operation> ops = new ArrayList<>(diff.getRootOperations());
            ops.sort(Comparator.comparingInt(o -> o.getSrcNode().getPosition().getSourceStart()));
            System.out.printf("[%s]%n", ops.stream()
                    //.map(Object::getClass)
                    //.map(Class::getName)
                    .map(op -> op.getClass().getName() + " " + op.getSrcNode().toString())
                    //.map(cn -> cn.substring(1 + cn.lastIndexOf('.')))
                    .collect(Collectors.joining(", ")));
            if (!ops.isEmpty()) {
                /* try regular expressions handlers */
                for (final RegExpHandler regExpHandler : this.regExpHandlers) {
                    regExpHandler.reset();
                    ListIterator<Operation> opLIt = ops.listIterator();
                    Status preStatus = null;
                    while (opLIt.hasNext()) {
                        final Operation operation = opLIt.next();
                        final Status curStatus = regExpHandler.handle(operation);
                        System.out.println(curStatus);
                        int count = regExpHandler.getConsumed();
                        if (preStatus == Status.CANDIDATE && curStatus == Status.REJECTED) {
                            while (count-- > 0) { // go back for count steps. one step will be
                                opLIt.previous(); // compensated by the call to method next()
                            }                     // in the next iteration.
                            regExpHandler.reset();
                        } else if (curStatus == Status.ACCEPTED) {
                            while (count-- > 0) { // delete last count operations
                                opLIt.remove();
                                if (opLIt.hasPrevious()) {
                                    opLIt.previous();
                                }
                            }
                            System.out.println(">>> " + regExpHandler.getRule().getClass().getName());
                            regExpHandler.reset();
                        }
                        preStatus = curStatus;
                    }
                }
                final IfShortCircuitHandler ifShortCircuitHandler = new IfShortCircuitHandler();
                for (final Operation op : ops) {
                    ifShortCircuitHandler.handle(op);
//                    for (final OperationHandler handler : this.pointHandlers) {
//                        if (handler != null && handler.canHandleOperation(op)) {
//                            handler.handleOperation(op);
//                        }
//                    }
                }
            } else {
                out.println("warning: no diff was found.");
            }
        } catch (Exception  e) {
            out.printf("warning: \'%s\' swallowed.%n", e.getMessage());
            e.printStackTrace();
        }
    }
}
