package edu.utdallas.fpm.main;

import edu.utdallas.fpm.commons.Util;
import edu.utdallas.fpm.pattern.rules.*;
import edu.utdallas.fpm.pattern.rules.prapr.PraPRRuleFactory;

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.BlockingQueue;

public abstract class StatisticsRenderer extends Consumer {
    /* rule-name --> count */
    private final Map<String, Integer> table;

    private StatisticsRenderer(BlockingQueue<Rule> queue) {
        super(queue);
        this.table = new HashMap<>();
    }

    public static StatisticsRenderer build(BlockingQueue<Rule> queue) {
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
    protected void consume(Rule rule) {
        final Rule specialized = PraPRRuleFactory.specialize(rule);
        final String ruleId = specialized == null ? rule.getId() :  "*" + specialized.getId();
        this.table.compute(ruleId, (k, v) -> v == null ? 1 : 1 + v);
    }

    @Override
    protected void cleanup() {
        System.out.println("Presenting statistics...");
        try (final PrintWriter pwGeneralPatterns = new PrintWriter("out-general.csv")) {
            this.table.entrySet().stream()
                    .sorted(Comparator.comparingInt(Map.Entry<String, Integer>::getValue).reversed())
                    .forEach(ent -> {
                        final int occurrenceCount = ent.getValue();
                        final String ruleId = ent.getKey();
                        pwGeneralPatterns.printf("%s,%d%n", ruleId, occurrenceCount);
                    });
        } catch (Exception e) {
            Util.panic(e);
        }
    }
}
