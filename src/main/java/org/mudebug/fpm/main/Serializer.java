package org.mudebug.fpm.main;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.mudebug.fpm.pattern.rules.Rule;

import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.zip.GZIPOutputStream;

import static org.mudebug.fpm.commons.Util.panic;

/* this will gracefully die if it see a null value in the queue */
public class Serializer extends Thread {
    private static final Pair<Rule, String> END =
            new ImmutablePair<>(null, null);
    private final BlockingQueue<Pair<Rule, String>> queue;
    private OutputStream os;
    private ObjectOutputStream oos;

    private Serializer(BlockingQueue<Pair<Rule, String>> queue,
                       OutputStream os) {
        this.queue = queue;
        try {
            this.os = os;
            this.oos = new ObjectOutputStream(os);
        } catch (Exception e) {
            panic(e);
        }
    }

    public static Serializer build(BlockingQueue<Pair<Rule, String>> queue,
                            File file,
                            boolean compressed) {
        try {
            OutputStream core = new FileOutputStream(file);
            if (compressed) {
                core = new GZIPOutputStream(core);
            }
            OutputStream os = new BufferedOutputStream(core);
            final Serializer serializer = new Serializer(queue, os);
            serializer.start();
            return serializer;
        } catch (Exception e) {
            panic(e);
        }
        return null;
    }

    @Override
    public void run() {
        try {
            while (true) {
                final Pair<Rule, String> pair = queue.take();
                if (pair == END) {
                    break; // end the thread
                }
                this.oos.writeObject(pair);
            }
        } catch (Exception e) {
            panic(e);
        } finally {
            try {
                this.oos.flush();
                this.oos.close();
                this.os.close();
            } catch (Exception e) {
                /* who cares?! */
            }
        }
    }

    public void kill() throws InterruptedException {
        this.queue.add(END);
        join();
    }
}
