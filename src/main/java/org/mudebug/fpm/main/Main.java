package org.mudebug.fpm.main;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.Operation;
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
import org.mudebug.fpm.pattern.rules.prapr_specializations.*;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.cu.position.NoSourcePosition;

import static java.lang.System.out;

public final class Main implements FilePairVisitor {
    private final RegExpHandler[] regExpHandlers;
    private final OperationHandler[] pointHandlers;
    /*1: the rule, 2: project name*/
    private final List<Pair<Rule, String>> table;

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
                new FieldLocalReplacementHandler(),
                new LocalToMethodReplacementHandler(),
                new MethodToLocalReplacementHandler(),
                new NegateIntExpHandler(),
                new NegateConditionalHandler(),
                new FieldMethDerefGuardHandler(),
                new AccessorHandler(),
                new SimpleMethCallGuardHandler()
        };
        this.table = new ArrayList<>();
    }

    private static boolean isSwitch(final String str) {
        return str.trim().equalsIgnoreCase("-p")
                || str.trim().equalsIgnoreCase("--parallel");
    }

    public static void main(String[] args) {
        final FileListParser parser;
        boolean parallelInvocation = false;
        final String fileName;
        if (args.length > 2) {
            out.println("fatal: too many arguments");
            return;
        } else if (args.length < 1) { // no file is specified, no parallelism
            parser = new FileListParser();
        } else if (args.length == 2) {
            if (isSwitch(args[0]) ^ isSwitch(args[1])) {
                fileName = isSwitch(args[0]) ? args[1] : args[0];
                parser = new FileListParser(new File(fileName));
                parallelInvocation = true;
            } else {
                out.println("fatal: illegal arguments");
                return;
            }
        } else if (isSwitch(args[0])) {
            parser = new FileListParser();
            parallelInvocation = true;
        } else {
            fileName = args[0];
            parser = new FileListParser(new File(fileName));
        }
        final Main visitor = new Main();
        parser.parse(visitor, parallelInvocation);
        out.println("presenting the results...");
        try (final PrintWriter pwGeneralPatterns = new PrintWriter("out-general.csv");
             final PrintWriter pwProjectsCount = new PrintWriter("out-projects.csv")) {
            visitor.table.stream()
                    .map(Main::praprSpecialize)
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(p -> p.getLeft().getClass().getName()))
                    .entrySet().stream()
                    .sorted((ent1, ent2) -> Integer.compare(ent2.getValue().size(), ent1.getValue().size()))
                    .forEach(ent -> {
                        final int projectsCount = (int) ent.getValue().stream()
                                .map(p -> p.getRight())
                                .distinct()
                                .count();
                        final int occurrenceCount = ent.getValue().size();
                        final String ruleClassName = ent.getKey();
                        final String ruleID = ruleClassName.substring(1 + ruleClassName.lastIndexOf('.'));
                        pwGeneralPatterns.printf("%s,%d%n", ruleID, occurrenceCount);
                        pwProjectsCount.printf("%s,%d%n", ruleID, projectsCount);
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Pair<Rule, String> praprSpecialize(final Pair<Rule, String> raw) {
        final Rule rawRule = raw.getLeft();
        if (rawRule instanceof ConstantReplacementRule) {
            final ConstantReplacementRule crr = (ConstantReplacementRule) rawRule;
            Rule specialized = InlineConstantMutatorRule.build(crr);
            if (specialized == null) {
                specialized = InvertNegsMutatorRule.build(crr);
            }
            if (specialized == null) {
                return null;
            }
            return new ImmutablePair<>(specialized, raw.getRight());
        } else if (rawRule instanceof BinaryOperatorDeletedRule) {
            final BinaryOperatorDeletedRule dobr = (BinaryOperatorDeletedRule) rawRule;
            final Rule specialized = AODRule.build(dobr);
            if (specialized == null) {
                return null;
            }
            return new ImmutablePair<>(specialized, raw.getRight());
        } else if (rawRule instanceof BinaryOperatorReplacementRule) {
            final BinaryOperatorReplacementRule borr =
                    (BinaryOperatorReplacementRule) rawRule;
            Rule specialized = MathMutatorRule.build(borr);
            if (specialized == null) {
                specialized = AORRule.build(borr);
                if (specialized == null) {
                    specialized = ConditionalBoundaryMutatorRule.build(borr);
                    if (specialized == null) {
                        specialized = RORRule.build(borr);
                        if (specialized == null) {
                            return null;
                        }
                    }
                }
            }
            return new ImmutablePair<>(specialized, raw.getRight());
        } else if ((rawRule instanceof FieldReadToLocalReadRule)
                || (rawRule instanceof FieldWriteToLocalWrite)) {
            final Rule specialized = new FieldAccessToLocalAccessMutatorRule();
            return new ImmutablePair<>(specialized, raw.getRight());
        } else if ((rawRule instanceof LocalReadToFieldReadRule)
                || (rawRule instanceof LocalWriteToFieldWriteRule)) {
            final Rule specialized = new LocalToFieldAccessMutatorRule();
            return new ImmutablePair<>(specialized, raw.getRight());
        }
        return raw;
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
//                final String temp = ops.stream()
//                        .map(operation -> operation.getClass().getName())
//                        .map(cn -> cn.substring(1 + cn.lastIndexOf('.')))
//                        .collect(Collectors.joining(","));
//                System.out.printf("[%s]%n", temp);
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
//                            System.out.println("**match** " + theRule.getClass().getName());
                            this.table.add(new ImmutablePair<>(theRule, projectName));
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
//                                System.out.println("**match** " + rule.getClass().getName());
                                this.table.add(new ImmutablePair<>(rule, projectName));
                            }
                        }
                    }
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
