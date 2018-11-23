package org.mudebug.fpm.main;

import java.io.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.Operation;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.mudebug.fpm.commons.FileListParser;
import org.mudebug.fpm.commons.FilePairVisitor;
import org.mudebug.fpm.pattern.handler.OperationHandler;
import org.mudebug.fpm.pattern.handler.point.delete.DeleteHandler;
import org.mudebug.fpm.pattern.handler.point.insert.InsertHandler;
import org.mudebug.fpm.pattern.handler.point.update.UpdateHandler;
import org.mudebug.fpm.pattern.handler.regexp.*;
import org.mudebug.fpm.pattern.rules.*;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.cu.position.NoSourcePosition;

import static org.mudebug.fpm.commons.Util.panic;

import static java.lang.System.out;

public final class Main implements FilePairVisitor {
    private final RegExpHandler[] regExpHandlers;
    private final OperationHandler[] pointHandlers;
    /*1: the rule, 2: project name*/
    private final BlockingQueue<Pair<Rule, String>> queue;
    private PrintWriter noDiffPW;

    private Main(BlockingQueue<Pair<Rule, String>> queue, boolean noDiff) {
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
                new FieldLocalReplacementHandler(),
                new LocalToMethodReplacementHandler(),
                new MethodToLocalReplacementHandler(),
                new NegateIntExpHandler(),
                new NegateConditionalHandler(),
                new FieldMethDerefGuardHandler(),
                new AccessorHandler(),
                new SimpleMethCallGuardHandler(),
                new IncDecRemovalHandler()
        };
        this.queue = queue;
        if (noDiff) {
            try {
                this.noDiffPW = new PrintWriter("no-diffs.csv");
            } catch (Exception e) {
                panic(e);
            }
        }
    }

    private static void printHelp(final Options options) {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("fix-pattern-miner", options);
    }

    private void cleanup() {
        if (this.noDiffPW != null) {
            this.noDiffPW.close();
        }
    }

    public static void main(String[] args) throws Exception {
        final Options options = new Options();

        options.addOption("p", "parallel", false, "parallel diff and mining");
        options.addOption("s", "serialize", true, "write rules on disk");
        options.addOption("c", "compress", false, "compressed output file");
        options.addOption("f", "file", true, "input CSV file");
        options.addOption("k", "fake", false,
                "prints all the file pairs for which GumTree failed to file diff");
        options.addOption("h", "help", false, "prints this help message");

        final CommandLineParser commandLineParser = new DefaultParser();
        final CommandLine cmd = commandLineParser.parse(options, args);

        if (cmd.getArgs().length > 0) {
            out.println("fatal: too many arguments");
            out.println();
            printHelp(options);
            return;
        }

        if (cmd.hasOption("h")) {
            printHelp(options);
            return;
        }
        /* blocking queues are thread-safe */
        final BlockingQueue<Pair<Rule, String>> queue = new LinkedBlockingDeque<>();
        final Consumer queueConsumer;

        if (cmd.hasOption("s")) {
            final boolean compress = cmd.hasOption("c");
            queueConsumer = Serializer.build(queue,
                    new File(cmd.getOptionValue("s")),
                    compress);
        } else {
            queueConsumer = StatisticsRenderer.build(queue);
        }

        final FileListParser parser;

        if (cmd.hasOption("f")) {
            final String fileName = cmd.getOptionValue("f");
            parser = new FileListParser(new File(fileName));
        } else {
            parser = new FileListParser();
        }

        final boolean noDiff = cmd.hasOption("k");
        final Main visitor = new Main(queue, noDiff);
        final boolean parallelInvocation = cmd.hasOption("p");

        parser.parse(visitor, parallelInvocation);

        visitor.cleanup();
        queueConsumer.kill();
    }

    private void reportNoDiff(final File buggy, final File fixed) {
        out.println("warning: no diff was found.");
        if (this.noDiffPW != null) {
            synchronized (this.noDiffPW) {
                this.noDiffPW.printf("%s,%s%n",
                        buggy.getAbsolutePath(),
                        fixed.getAbsolutePath());
            }
        }
    }

    @Override
    public void visit(final File buggy, final File fixed) {
        out.printf("Diffing (%s):%n\t%s%n\t%s%n",
                buggy.getParent(),
                buggy.getName(),
                fixed.getName());
        final AstComparator ac = new AstComparator();
        try {
            final Diff diff = ac.compare(buggy, fixed);
            final List<Operation> ops = new ArrayList<>(diff.getRootOperations());
            ops.sort(Comparator.comparingInt(o -> {
                final SourcePosition sp = o.getSrcNode().getPosition();
                if (sp instanceof NoSourcePosition) {
                    return Integer.MAX_VALUE;
                }
                return sp.getSourceStart();
            }));
            if (!ops.isEmpty()) {
                /* try regular expressions handlers */
                for (final RegExpHandler regExpHandler : this.regExpHandlers) {
                    regExpHandler.reset();
                    ListIterator<Operation> opLIt = ops.listIterator();
                    Status preStatus = null;
                    while (opLIt.hasNext()) {
                        final Operation operation = opLIt.next();
                        final Status curStatus = regExpHandler.handle(operation);
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
                            final Rule theRule = regExpHandler.getRule();
                            final String projectName = buggy.getAbsolutePath();
                            this.queue.add(new ImmutablePair<>(theRule, projectName));
                            regExpHandler.reset();
                        }
                        preStatus = curStatus;
                    }
                }
                for (final Operation op : ops) {
                    for (final OperationHandler handler : this.pointHandlers) {
                        if (handler != null && handler.canHandleOperation(op)) {
                            final Rule rule = handler.handleOperation(op);
                            if (!(rule instanceof UnknownRule)) {
                                final String projectName = buggy.getAbsolutePath();
                                this.queue.add(new ImmutablePair<>(rule, projectName));
                            }
                        }
                    }
                }
            } else {
                reportNoDiff(buggy, fixed);
            }
        } catch (Exception  e) {
            out.printf("warning: \'%s\' swallowed.%n", e.getMessage());
            // e.printStackTrace();
        }
    }
}
