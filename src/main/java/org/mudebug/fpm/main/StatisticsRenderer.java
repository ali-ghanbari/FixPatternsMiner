package org.mudebug.fpm.main;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.mudebug.fpm.pattern.rules.*;
import org.mudebug.fpm.pattern.rules.prapr_specializations.*;

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.function.ToIntFunction;

import static org.mudebug.fpm.commons.Util.panic;

public abstract class StatisticsRenderer extends Consumer {
    /* rule-name --> (count, {project-names}) */
    private final Map<String, Pair<MutableInt, Set<String>>> table;

    private StatisticsRenderer(BlockingQueue<Pair<Rule, String>> queue) {
        super(queue);
        this.table = new HashMap<>();
    }

    public static StatisticsRenderer build(BlockingQueue<Pair<Rule, String>> queue) {
        final StatisticsRenderer statisticsRenderer = new StatisticsRenderer(queue) {
            final Thread me = new Thread(this);

            @Override
            protected Thread getMe() {
                return this.me;
            }
        };
        statisticsRenderer.start();
        return statisticsRenderer;
    }

    @Override
    protected void consume(Pair<Rule, String> pair) {
        pair = praprSpecialize(pair);
        if (pair == null) {
            return;
        }
        final String ruleId = pair.getLeft().getClass().getSimpleName();
        Pair<MutableInt, Set<String>> info = this.table.get(ruleId);
        if (info == null) {
            info = new ImmutablePair<>(new MutableInt(0), new HashSet<>());
        }
        info.getLeft().increment();
        final String projectName = pair.getRight();
        info.getRight().add(projectName);
        this.table.put(ruleId, info);
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
                            specialized = NegatedConditionalMutatorRule.build(borr);
                            if (specialized == null) {
                                return null;
                            }
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
        } else if (rawRule instanceof NegatedConditionalExprRule) {
            final NegatedConditionalExprRule ncer =
                    (NegatedConditionalExprRule) rawRule;
            final Rule specialized = NegatedConditionalMutatorRule.build(ncer);
            return new ImmutablePair<>(specialized, raw.getRight());
        } else if (rawRule instanceof ArgumentPropagatedRule) {
            final ArgumentPropagatedRule apr = (ArgumentPropagatedRule) rawRule;
            final Rule specialized = NakedReceiverMutatorRule.build(apr);
            if (specialized != null) {
                return new ImmutablePair<>(specialized, raw.getRight());
            }
        }
        return raw;
    }

    @Override
    protected void cleanup() {
        System.out.println("Presenting statistics...");
        final ToIntFunction<Map.Entry<String, Pair<MutableInt, Set<String>>>> getCount =
                e -> e.getValue().getLeft().intValue();
        try (final PrintWriter pwGeneralPatterns = new PrintWriter("out-general.csv");
             final PrintWriter pwProjectsCount = new PrintWriter("out-projects.csv")) {
            this.table.entrySet().stream()
                    .sorted(Comparator.comparingInt(getCount).reversed())
                    .forEach(ent -> {
                        final int projectsCount = ent.getValue().getRight().size();
                        final int occurrenceCount = getCount.applyAsInt(ent);
                        final String ruleId = ent.getKey();
                        pwGeneralPatterns.printf("%s,%d%n", ruleId, occurrenceCount);
                        pwProjectsCount.printf("%s,%d%n", ruleId, projectsCount);
                    });
        } catch (Exception e) {
            panic(e);
        }
    }
}
