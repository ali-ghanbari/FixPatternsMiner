package edu.utdallas.fpm.main;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import edu.utdallas.fpm.commons.FileListParser;
import edu.utdallas.fpm.commons.FilePairVisitor;
import edu.utdallas.fpm.pattern.handler.regexp.*;
import edu.utdallas.fpm.pattern.rules.Rule;
import edu.utdallas.fpm.pattern.rules.UnknownRule;
import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.Operation;
import org.apache.commons.cli.*;
import edu.utdallas.fpm.pattern.handler.OperationHandler;
import edu.utdallas.fpm.pattern.handler.point.delete.DeleteHandler;
import edu.utdallas.fpm.pattern.handler.point.insert.InsertHandler;
import edu.utdallas.fpm.pattern.handler.point.update.UpdateHandler;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.cu.position.NoSourcePosition;

import static java.lang.System.out;

import static edu.utdallas.fpm.commons.Util.*;

public final class Main implements FilePairVisitor {
    private final RegExpHandler[] regExpHandlers;
    private final OperationHandler[] pointHandlers;
    private final BlockingQueue<Rule> rulesQueue;
    private PrintWriter noDiffPW;
    private PrintWriter timedOutDiffPW;
    private final ExecutorService executorService;
    private final int timeout;

    private Main(BlockingQueue<Rule> rulesQueue,
                 boolean debug,
                 int timeout) {
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
        this.rulesQueue = rulesQueue;
        if (debug) {
            try {
                this.noDiffPW = new PrintWriter("no-diffs.csv");
                this.timedOutDiffPW = new PrintWriter("timed-out.csv");
            } catch (Exception e) {
                panic(e);
            }
        }
        this.executorService = Executors.newSingleThreadExecutor();
        this.timeout = timeout;
    }

    private static void printHelp(final Options options) {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("fix-pattern-miner", options);
    }

    private void cleanup() {
        if (this.noDiffPW != null) {
            this.noDiffPW.close();
        }
        if (this.timedOutDiffPW != null) {
            this.timedOutDiffPW.close();
        }
        this.executorService.shutdownNow();
    }

    public static void main(String[] args) throws Exception {
        final Options options = new Options();

        options.addOption("p", "parallel", false, "parallel diff and mining");
        options.addOption("f", "file", true, "input CSV file");
        options.addOption("d", "debug", false, "output timed-out and ineffective diffs");
        options.addOption("t", "diff-timeout", true, "diffing timeout in seconds");
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
        final BlockingQueue<Rule> queue = new LinkedBlockingDeque<>();

        final Consumer queueConsumer = StatisticsRenderer.build(queue);

        final FileListParser parser;

        if (cmd.hasOption("f")) {
            final String fileName = cmd.getOptionValue("f");
            parser = new FileListParser(new File(fileName));
        } else {
            parser = new FileListParser();
        }

        final boolean debug = cmd.hasOption("d");
        int timeout;
        if (cmd.hasOption("t")) {
            timeout = Integer.parseInt(cmd.getOptionValue("t"));
            if (timeout <= 0) {
                out.println("fatal: illegal timeout value");
                out.println();
                printHelp(options);
                return;
            }
        } else {
            timeout = -1;
        }

        final Main visitor = new Main(queue, debug, timeout);
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

    private void reportTimeOut(final File buggy, final File fixed) {
        out.println("warning: diffing timed-out!");
        if (this.timedOutDiffPW != null) {
            synchronized (this.timedOutDiffPW) {
                this.timedOutDiffPW.printf("%s,%s%n",
                        buggy.getAbsolutePath(),
                        fixed.getAbsolutePath());
            }
        }
    }

    private List<Operation> safeDiff(final File buggy,
                                     final File fixed,
                                     int timeout) {
        final Future<List<Operation>> diffTask;
        synchronized (this.executorService) {
            diffTask = this.executorService.submit(() -> {
                final AstComparator ac = new AstComparator();
                try {
                    final Diff diff = ac.compare(buggy, fixed);
                    final List<Operation> ops = new ArrayList<>(diff.getRootOperations());
                    return new ArrayList<>(diff.getRootOperations());
                } catch (Exception e) {
                    out.printf("warning: \'%s\' swallowed.%n", e.getMessage());
                    return Collections.emptyList();
                }
            });
        }
        try {
            if (timeout < 0) {
                return diffTask.get();
            }
            return diffTask.get(timeout, TimeUnit.SECONDS);
        } catch (TimeoutException te) {
            reportTimeOut(buggy, fixed);
            if (!diffTask.cancel(true)) {
                out.println("warning: a diff thread was not cancellable");
            }
        } catch (InterruptedException | CancellationException | ExecutionException e) {
            out.printf("warning: \'%s\' swallowed.%n", e.getMessage());
        }
        return Collections.emptyList();
    }

    @Override
    public void visit(final File buggy, final File fixed) {
        out.printf("Queue Size = %d%n", this.rulesQueue.size());
        out.printf("Diffing (%s):%n\t%s%n\t%s%n",
                buggy.getParentFile().getParent(),
                buggy.getName(),
                fixed.getName());

        final List<Operation> ops = safeDiff(buggy, fixed, this.timeout);

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
                        if (!(theRule instanceof UnknownRule)) {
                            this.rulesQueue.offer(theRule);
                        }
                        regExpHandler.reset();
                    }
                    preStatus = curStatus;
                }
            }
            for (final Operation op : ops) {
                for (final OperationHandler handler : this.pointHandlers) {
                    if (handler != null && handler.canHandleOperation(op)) {
                        final Rule theRule = handler.handleOperation(op);
                        if (!(theRule instanceof UnknownRule)) {
                            this.rulesQueue.offer(theRule);
                        }
                    }
                }
            }
        } else {
            reportNoDiff(buggy, fixed);
        }
    }
}
