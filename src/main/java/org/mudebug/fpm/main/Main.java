package org.mudebug.fpm.main;

import java.io.*;
import java.util.List;

import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.Operation;
import org.mudebug.fpm.commons.FileListParser;
import org.mudebug.fpm.commons.FilePairVisitor;
import org.mudebug.fpm.pattern.handler.OperationHandler;
import org.mudebug.fpm.pattern.handler.delete.DeleteHandler;
import org.mudebug.fpm.pattern.handler.insert.InsertHandler;
import org.mudebug.fpm.pattern.handler.update.UpdateHandler;

import static java.lang.System.out;

public final class Main implements FilePairVisitor {
    private final OperationHandler[] handlers;

    private Main() {
        this.handlers = new OperationHandler[] {
                DeleteHandler.createHandlerChain(),
                InsertHandler.createHandlerChain(),
                UpdateHandler.createHandlerChain()
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
        parser.parse(visitor, true);
    }

    @Override
    public void visit(final File buggy, final File fixed) {
        out.printf("Diffing (%s):%n\t%s%n\t%s%n", buggy.getParent(), buggy.getName(), fixed.getName());
        final AstComparator ac = new AstComparator();
        try {
            final Diff diff = ac.compare(buggy, fixed);
            final List<Operation> ops = diff.getRootOperations();
            if (!ops.isEmpty()) {
                for (final Operation op : ops) {
                    for (final OperationHandler handler : this.handlers) {
                        if (handler != null && handler.canHandleOperation(op)) {
                            handler.handleOperation(op);
                        }
                    }
                }
            } else {
                out.println("warning: no diff was found.");
            }
        } catch (Exception  e) {
            out.printf("warning: \'%s\' swallowed.%n", e.getMessage());
        }
    }
}
