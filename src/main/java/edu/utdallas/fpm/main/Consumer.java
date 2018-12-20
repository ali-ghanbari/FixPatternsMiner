package edu.utdallas.fpm.main;

import edu.utdallas.fpm.commons.Util;
import edu.utdallas.fpm.pattern.rules.Rule;
import edu.utdallas.fpm.pattern.rules.UnknownRule;

import java.util.concurrent.BlockingQueue;

public abstract class Consumer implements Runnable {
    private static final Rule END = UnknownRule.UNKNOWN_RULE;
    private final BlockingQueue<Rule> queue;

    public Consumer(BlockingQueue<Rule> queue) {
        this.queue = queue;
    }

    public void kill() throws InterruptedException {
        this.queue.offer(END);
        getMe().join();
    }

    protected abstract Thread getMe();

    protected void start() {
        getMe().start();
    }

    protected abstract void consume(Rule rule) throws Exception;

    protected abstract void cleanup();

    @Override
    public void run() {
        try {
            while (true) {
                final Rule rule = this.queue.take();
                if (rule == END) {
                    break; // end the thread
                }
                consume(rule);
            }
        } catch (Exception e) {
            Util.panic(e);
        } finally {
            cleanup();
        }
    }
}
