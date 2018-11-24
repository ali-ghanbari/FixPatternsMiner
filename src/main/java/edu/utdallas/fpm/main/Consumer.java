package edu.utdallas.fpm.main;

import edu.utdallas.fpm.commons.Util;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import edu.utdallas.fpm.pattern.rules.Rule;

import java.util.concurrent.BlockingQueue;

public abstract class Consumer implements Runnable {
    private static final Pair<Rule, String> END = new ImmutablePair<>(null, null);
    private final BlockingQueue<Pair<Rule, String>> queue;

    public Consumer(BlockingQueue<Pair<Rule, String>> queue) {
        this.queue = queue;
    }

    public void kill() throws InterruptedException {
        this.queue.add(END);
        getMe().join();
    }

    protected abstract Thread getMe();

    protected void start() {
        getMe().start();
    }

    protected abstract void consume(Pair<Rule, String> pair) throws Exception;

    protected abstract void cleanup();

    @Override
    public void run() {
        try {
            while (true) {
                final Pair<Rule, String> pair = this.queue.take();
                if (pair == END) {
                    break; // end the thread
                }
                consume(pair);
            }
        } catch (Exception e) {
            Util.panic(e);
        } finally {
            cleanup();
        }
    }
}
