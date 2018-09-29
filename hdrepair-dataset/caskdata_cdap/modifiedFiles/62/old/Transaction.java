package com.continuuity.data2.transaction;

import com.google.common.base.Objects;

import java.util.Arrays;

/**
 * Transaction details
 */
// NOTE: this class should have minimal dependencies as it is used in HBase CPs and other places where minimal classes
//       are available
public class Transaction {
  private final long readPointer;
  private final long writePointer;
  private final long[] invalids;
  private final long[] inProgress;
  private final long firstShortInProgress;

  private static final long[] NO_EXCLUDES = { };
  public static final long NO_TX_IN_PROGRESS = Long.MAX_VALUE;

  public static final Transaction ALL_VISIBLE_LATEST =
    new Transaction(Long.MAX_VALUE, Long.MAX_VALUE, NO_EXCLUDES, NO_EXCLUDES, NO_TX_IN_PROGRESS);

  public Transaction(long readPointer, long writePointer, long[] invalids, long[] inProgress,
                     long firstShortInProgress) {
    this.readPointer = readPointer;
    this.writePointer = writePointer;
    this.invalids = invalids;
    this.inProgress = inProgress;
    this.firstShortInProgress = firstShortInProgress;
  }

  public long getReadPointer() {
    return readPointer;
  }

  public long getWritePointer() {
    return writePointer;
  }

  public long[] getInvalids() {
    return invalids;
  }

  public long[] getInProgress() {
    return inProgress;
  }

  public long getFirstInProgress() {
    return inProgress.length == 0 ? NO_TX_IN_PROGRESS : inProgress[0];
  }

  public long getFirstShortInProgress() {
    return firstShortInProgress;
  }

  public boolean isInProgress(long version) {
    return Arrays.binarySearch(inProgress, version) >= 0;
  }

  public boolean isExcluded(long version) {
    return Arrays.binarySearch(inProgress, version) >= 0
      || Arrays.binarySearch(invalids, version) >= 0;
  }

  public boolean isVisible(long version) {
    return version <= getReadPointer() && !isExcluded(version);
  }

  public boolean hasExcludes() {
    return invalids.length > 0 || inProgress.length > 0;
  }


  public int excludesSize() {
    return invalids.length + inProgress.length;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
                  .add("readPointer", readPointer)
                  .add("writePointer", writePointer)
                  .add("invalids", Arrays.toString(invalids))
                  .add("inProgress", Arrays.toString(inProgress))
                  .toString();
  }
}
