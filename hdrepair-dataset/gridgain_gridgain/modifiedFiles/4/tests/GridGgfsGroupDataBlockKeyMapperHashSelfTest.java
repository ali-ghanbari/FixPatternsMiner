/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.ggfs;

import org.gridgain.grid.*;
import org.gridgain.grid.ggfs.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.util.concurrent.*;

/**
 * Tests for {@link GridGgfsGroupDataBlocksKeyMapper} hash.
 */
public class GridGgfsGroupDataBlockKeyMapperHashSelfTest extends GridGgfsCommonAbstractTest {
    /**
     * @throws Exception If failed.
     */
    // TODO Enable after GG-9153.
    public void _testDistribution() throws Exception {
        for (int i = 0; i < 100; i++) {
            int grpSize = ThreadLocalRandom.current().nextInt(2, 100000);
            int partCnt = ThreadLocalRandom.current().nextInt(1, grpSize);

            checkDistribution(grpSize, partCnt);
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testIntOverflowDistribution() throws Exception {
        for (int i = 0; i < 100; i++)
            checkIntOverflowDistribution(ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE));
    }

    /**
     * Check hash code generation for the given group size and partitions count.
     *
     * @throws Exception If failed.
     */
    public void checkDistribution(int grpSize, int partCnt) throws Exception {
        GridUuid fileId = GridUuid.randomUuid();

        GridGgfsGroupDataBlocksKeyMapper mapper = new GridGgfsGroupDataBlocksKeyMapper(grpSize);

        int lastPart = 0;

        boolean first = true;

        for (int i = 0; i < 10; i++) {
            // Ensure that all blocks within the group has the same hash codes.
            boolean firstInGroup = true;

            for (int j = 0; j < grpSize; j++) {
                GridGgfsBlockKey key = new GridGgfsBlockKey(fileId, null, false, i * grpSize + j);

                Integer part = (Integer) mapper.affinityKey(key) % partCnt;

                if (firstInGroup) {
                    if (first)
                        first = false;
                    else
                        checkPartition(lastPart, part, partCnt);

                    firstInGroup = false;
                }
                else
                    assert part == lastPart;

                lastPart = part;
            }
        }
    }

    /**
     * Check distribution for integer overflow.
     *
     * @throws Exception If failed.
     */
    @SuppressWarnings("NumericOverflow")
    public void checkIntOverflowDistribution(int partCnt) throws Exception {
        GridUuid fileId = GridUuid.randomUuid();

        GridGgfsGroupDataBlocksKeyMapper mapper = new GridGgfsGroupDataBlocksKeyMapper(1);

        Integer part1 = (Integer) mapper.affinityKey(new GridGgfsBlockKey(fileId, null, false,
            Integer.MAX_VALUE - 1)) % partCnt;
        Integer part2 = (Integer) mapper.affinityKey(new GridGgfsBlockKey(fileId, null, false,
            Integer.MAX_VALUE)) % partCnt;
        Integer part3 = (Integer) mapper.affinityKey(new GridGgfsBlockKey(fileId, null, false,
            (long)Integer.MAX_VALUE + 1)) % partCnt;

        checkPartition(part1, part2, partCnt);
        checkPartition(part2, part3, partCnt);
    }

    /**
     * Check correct partition shift.
     *
     * @param prevPart Previous partition.
     * @param part Current partition.
     * @param totalParts Total partitions.
     */
    private void checkPartition(int prevPart, int part, int totalParts) {
        assert U.safeAbs(prevPart - part) == 1 || (part == 0 && prevPart == totalParts - 1) :
            "Total=" + totalParts + ", prevPart=" + prevPart + ", part=" + part + ']';
    }
}

