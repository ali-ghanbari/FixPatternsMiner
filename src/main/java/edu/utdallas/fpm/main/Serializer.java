package edu.utdallas.fpm.main;

import edu.utdallas.fpm.commons.Util;
import org.apache.commons.lang3.tuple.Pair;
import edu.utdallas.fpm.pattern.rules.Rule;

import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.zip.GZIPOutputStream;

/* this will gracefully die if it sees a null value in the queue */
public abstract class Serializer extends Consumer {
    private ObjectOutputStream oos;
    private OutputStream os;

    private Serializer(BlockingQueue<Pair<Rule, String>> queue,
                       OutputStream os) throws Exception {
        super(queue);
        this.oos = new ObjectOutputStream(os);
        this.os = os;
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
            final Serializer serializer = new Serializer(queue, os) {
                final Thread me = new Thread(this);
                @Override
                protected Thread getMe() {
                    return this.me;
                }
            };
            serializer.start();
            return serializer;
        } catch (Exception e) {
            Util.panic(e);
        }
        return null;
    }

    @Override
    protected void consume(Pair<Rule, String> pair) throws Exception {
        this.oos.writeObject(pair);
    }

    @Override
    protected void cleanup() {
        try {
            this.oos.flush();
            this.oos.close();
            this.os.close();
        } catch (Exception e) {
            /* who cares?! */
        }
    }
}
