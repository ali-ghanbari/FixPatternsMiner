package org.mudebug.fpm.main;

import java.io.*;
import java.util.List;

import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.Operation;
import org.mudebug.fpm.commons.FileListParser;
import org.mudebug.fpm.commons.FilePairVisitor;

import static java.lang.System.out;

public class Main implements FilePairVisitor {
    private Main() {

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
        parser.parse(visitor);
    }

    @Override
    public void visit(final File buggy, final File fixed) {
        out.printf("Diffing:%n\t%s%n\t%s%n", buggy.getName(), fixed.getName());
        final AstComparator ac = new AstComparator();
        try {
            final Diff diff = ac.compare(buggy, fixed);
            final List<Operation> ops = diff.getRootOperations();
            if (!ops.isEmpty()) {

            } else {
                out.println("warning: no diff found.");
            }
        } catch (Exception  e) {
            out.printf("warning: \'%s\' swallowed.%n", e.getMessage());
        }
    }
}
