package net.openhft.chronicle.queue.impl;

import net.openhft.chronicle.queue.*;
import net.openhft.chronicle.wire.BinaryWire;
import net.openhft.chronicle.wire.ValueOut;
import net.openhft.chronicle.wire.WireKey;
import net.openhft.chronicle.wire.WireOut;
import net.openhft.lang.Jvm;
import net.openhft.lang.io.*;
import net.openhft.lang.model.DataValueClasses;
import net.openhft.lang.values.LongValue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static net.openhft.chronicle.wire.BinaryWire.isDocument;

/**
 * SingleChronicle implements Chronicle over a single streaming file
 * <p>
 * Created by peter on 30/01/15.
 */
public class SingleChronicleQueue implements ChronicleQueue, DirectChronicleQueue {

    // don't write to this without reviewing net.openhft.chronicle.queue.impl.SingleChronicleQueue.casMagicOffset
    private static final long MAGIC_OFFSET = 0L;

    static final long HEADER_OFFSET = 8L;
    static final long UNINITIALISED = 0L;
    static final long BUILDING = asLong("BUILDING");
    static final long QUEUE_CREATED = asLong("QUEUE400");
    static final int NOT_READY = 1 << 30;
    static final int META_DATA = 1 << 31;
    static final int LENGTH_MASK = -1 >>> 2;
    static final int MAX_LENGTH = LENGTH_MASK;

    private final ThreadLocal<ExcerptAppender> localAppender = new ThreadLocal<>();
    private final MappedFile file;
    private final MappedMemory headerMemory;
    private final Header header = new Header();
    private final ChronicleWire wire;
    private final Bytes bytes;
    private long firstBytes = -1;

    public SingleChronicleQueue(String filename, long blockSize) throws IOException {
        file = new MappedFile(filename, blockSize);
        headerMemory = file.acquire(0);
        bytes = headerMemory.bytes();
        wire = new ChronicleWire(new BinaryWire(bytes));
        initialiseHeader();
    }

    @Override
    public long appendDocument(Bytes buffer) {
        long length = buffer.remaining();
        if (length > MAX_LENGTH)
            throw new IllegalStateException("Length too large: " + length);
        LongValue writeByte = header.writeByte;
        long lastByte = writeByte.getVolatileValue();

        for (; ; ) {
            if (bytes.compareAndSwapInt(lastByte, 0, NOT_READY | (int) length)) {
                long lastByte2 = lastByte + 4 + buffer.remaining();
                bytes.write(lastByte + 4, buffer);
                long lastIndex = header.lastIndex.addAtomicValue(1);
                writeByte.setValue(lastByte2);
                bytes.writeOrderedInt(lastByte, (int) length);
                return lastIndex;
            }
            int length2 = length30(bytes.readVolatileInt());
            bytes.skip(length2);
            Jvm.checkInterrupted();
        }

    }

    @Override
    public boolean readDocument(AtomicLong offset, Bytes buffer) {
        buffer.clear();
        long lastByte = offset.get();
        for (; ; ) {
            int length = bytes.readVolatileInt(lastByte);
            int length2 = length30(length);
            if (BinaryWire.isReady(length)) {
                lastByte += 4;
                buffer.write(bytes, lastByte, length2);
                lastByte += length2;
                offset.set(lastByte);
                return isDocument(length);
            }
            Jvm.checkInterrupted();
        }
    }

    @Override
    public Bytes bytes() {
        return bytes;
    }

    @Override
    public long lastIndex() {
        long value = header.lastIndex.getVolatileValue();
        if (value == -1)
            throw new IllegalStateException("No data has been written to   chronicle.");
        return value;
    }

    @Override
    public boolean index(long index, MultiStoreBytes bytes) {
        if (index == -1) {
            bytes.storePositionAndSize(headerMemory, HEADER_OFFSET, headerMemory.size() - HEADER_OFFSET);
            return true;
        }
        return false;
    }

    private int length30(int i) {
        return i & LENGTH_MASK;
    }

    enum MetaDataKey implements WireKey {
        header, index2index, index
    }

    private void initialiseHeader() throws IOException {
        if (bytes.compareAndSwapLong(MAGIC_OFFSET, UNINITIALISED, BUILDING)) {
            buildHeader();
        }
        readHeader();
    }


    private void readHeader() throws IOException {
        // skip the magic number. 
        waitForTheHeaderToBeBuilt(bytes);

        bytes.position(HEADER_OFFSET);
        wire.readMetaData($ -> wire.read().readMarshallable(header));
        firstBytes = bytes.position();
    }

    private void waitForTheHeaderToBeBuilt(Bytes bytes) throws IOException {
        for (int i = 0; i < 1000; i++) {
            long magic = bytes.readVolatileLong(MAGIC_OFFSET);
            if (magic == BUILDING) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new IOException("Interrupted waiting for the header to be built");
                }
            } else if (magic == QUEUE_CREATED) {
                return;
            } else {
                throw new AssertionError("Invalid magic number " + Long.toHexString(magic) + " in file " + name());
            }
        }
        throw new AssertionError("Timeout waiting to build the file " + name());
    }

    private void buildHeader() {
        // skip the magic number.
        bytes.position(HEADER_OFFSET);

        wire.writeMetaData(() -> wire
                .write(MetaDataKey.header).writeMarshallable(header.init(Compression.NONE)));

        if (!bytes.compareAndSwapLong(MAGIC_OFFSET, BUILDING, QUEUE_CREATED))
            throw new AssertionError("Concurrent writing of the header");
    }

    static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            try {
                return Files.readAllLines(Paths.get("etc", "hostname")).get(0);
            } catch (Exception e2) {
                return "localhost";
            }
        }
    }

    private static long asLong(String str) {
        ByteBuffer bb = ByteBuffer.wrap(str.getBytes(StandardCharsets.ISO_8859_1)).order(ByteOrder.nativeOrder());
        return bb.getLong();
    }

    @Override
    public String name() {
        return file.name();
    }

    @Override
    public Excerpt createExcerpt() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ExcerptTailer createTailer() throws IOException {
        return new SingleTailer(this);
    }

    @Override
    public ExcerptAppender createAppender() throws IOException {
        ExcerptAppender appender = localAppender.get();
        if (appender == null)
            localAppender.set(appender = new SingleAppender(this));
        return appender;
    }

    @Override
    public long size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long firstAvailableIndex() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long lastWrittenIndex() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long firstBytes() {
        return firstBytes;
    }


    /**
     * @return gets the index2index, or creates it, if it does not exist.
     */
    long indexToIndex() {
        for (; ; ) {

            long index2Index = header.index2Index.getVolatileValue();

            if (index2Index == NOT_READY)
                continue;

            if (index2Index != UNINITIALISED)
                return index2Index;

            if (!header.index2Index.compareAndSwapValue(UNINITIALISED, NOT_READY))
                continue;

            long indexToIndex = newIndex();
            header.index2Index.setOrderedValue(indexToIndex);
            return indexToIndex;
        }
    }

    /**
     * creates a new Excerpt containing the index2index
     *
     * @return the address of the Excerpt
     */
    long newIndex() {

        // the space required for 17bits
        long wireLen = 6;
        long length = wireLen + 8L * (1L << 17L);

        long firstByte;
        LongValue writeByte = header.writeByte;
        long lastByte = writeByte.getVolatileValue();

        for (; ; ) {

            if (bytes.compareAndSwapInt(lastByte, 0, NOT_READY | (int) length)) {

                new BinaryWire(bytes.bytes(lastByte + 4, wireLen)).write(() -> "Index");
                firstByte = lastByte + 4 + wireLen;

                long lastByte2 = firstByte + length;
                header.lastIndex.addAtomicValue(1);
                writeByte.setValue(lastByte2);
                bytes.writeOrderedInt(lastByte, (int) length);
                long l = lastByte + 4 + wireLen + length;
                bytes.skip(length30((int)l));
                return firstByte;
            }

            int length2 = length30(bytes.readVolatileInt());
            bytes.skip(length2);
            Jvm.checkInterrupted();
        }
    }


}

