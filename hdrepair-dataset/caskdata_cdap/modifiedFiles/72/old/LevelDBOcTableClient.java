package com.continuuity.data2.dataset.lib.table.leveldb;

import com.continuuity.data.table.Scanner;
import com.continuuity.data2.dataset.lib.table.BackedByVersionedStoreOcTableClient;
import com.continuuity.data2.transaction.Transaction;

import java.io.IOException;
import java.util.Map;
import java.util.NavigableMap;

/**
 * A table client based on LevelDB.
 */
public class LevelDBOcTableClient extends BackedByVersionedStoreOcTableClient {

  private final LevelDBOcTableCore core;
  private Transaction tx;
  private long persistedVersion;

  public LevelDBOcTableClient(String tableName, LevelDBOcTableService service) throws IOException {
    super(tableName);
    this.core = new LevelDBOcTableCore(tableName, service);
  }

  // TODO this is the same for all OcTableClient implementations -> promote to base class
  @Override
  public void startTx(Transaction tx) {
    super.startTx(tx);
    this.tx = tx;
  }

  @Override
  protected void persist(NavigableMap<byte[], NavigableMap<byte[], byte[]>> changes) throws Exception {
    persistedVersion = tx == null ? System.currentTimeMillis() : tx.getWritePointer();
    core.persist(changes, persistedVersion);
  }

  @Override
  protected void undo(NavigableMap<byte[], NavigableMap<byte[], byte[]>> persisted) throws Exception {
    core.undo(persisted, persistedVersion);
  }

  @Override
  protected byte[] getPersisted(byte[] row, byte[] column) throws Exception {
    return core.getRow(row, new byte[][] { column }, null, null, 1, tx).get(column);
  }

  @Override
  protected NavigableMap<byte[], byte[]> getPersisted(byte[] row, byte[][] columns) throws Exception {
    return core.getRow(row, columns, null, null, columns.length, tx);
  }

  @Override
  protected NavigableMap<byte[], byte[]> getPersisted(byte[] row, byte[] startColumn, byte[] stopColumn, int limit)
    throws Exception {
    return core.getRow(row, null, startColumn, stopColumn, limit, tx);
  }

  @Override
  protected Scanner scanPersisted(byte[] startRow, byte[] stopRow) throws Exception {
    return core.scan(startRow, stopRow, tx);
  }
}
