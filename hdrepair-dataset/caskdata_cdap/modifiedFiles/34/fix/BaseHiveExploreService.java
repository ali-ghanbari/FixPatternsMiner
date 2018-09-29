/*
 * Copyright 2014 Cask, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.explore.service.hive;

import co.cask.cdap.common.conf.CConfiguration;
import co.cask.cdap.common.conf.Constants;
import co.cask.cdap.data2.dataset2.DatasetFramework;
import co.cask.cdap.explore.service.ExploreException;
import co.cask.cdap.explore.service.ExploreService;
import co.cask.cdap.explore.service.HandleNotFoundException;
import co.cask.cdap.explore.service.MetaDataInfo;
import co.cask.cdap.hive.context.CConfCodec;
import co.cask.cdap.hive.context.ConfigurationUtil;
import co.cask.cdap.hive.context.ContextManager;
import co.cask.cdap.hive.context.HConfCodec;
import co.cask.cdap.hive.context.TxnCodec;
import co.cask.cdap.proto.ColumnDesc;
import co.cask.cdap.proto.QueryHandle;
import co.cask.cdap.proto.QueryInfo;
import co.cask.cdap.proto.QueryResult;
import co.cask.cdap.proto.QueryStatus;
import com.continuuity.tephra.Transaction;
import com.continuuity.tephra.TransactionSystemClient;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.gson.Gson;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hive.service.cli.CLIService;
import org.apache.hive.service.cli.ColumnDescriptor;
import org.apache.hive.service.cli.FetchOrientation;
import org.apache.hive.service.cli.GetInfoType;
import org.apache.hive.service.cli.GetInfoValue;
import org.apache.hive.service.cli.HiveSQLException;
import org.apache.hive.service.cli.OperationHandle;
import org.apache.hive.service.cli.SessionHandle;
import org.apache.hive.service.cli.TableSchema;
import org.apache.hive.service.cli.thrift.TColumnValue;
import org.apache.hive.service.cli.thrift.TRow;
import org.apache.hive.service.cli.thrift.TRowSet;
import org.apache.twill.common.Threads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Defines common functionality used by different HiveExploreServices. The common functionality includes
 * starting/stopping transactions, serializing configuration and saving operation information.
 */
public abstract class BaseHiveExploreService extends AbstractIdleService implements ExploreService {
  private static final Logger LOG = LoggerFactory.getLogger(BaseHiveExploreService.class);
  private static final Gson GSON = new Gson();
  private static final int PREVIEW_COUNT = 5;

  private final CConfiguration cConf;
  private final Configuration hConf;
  private final HiveConf hiveConf;
  private final TransactionSystemClient txClient;

  // Handles that are running, or not yet completely fetched, they have longer timeout
  private final Cache<QueryHandle, OperationInfo> activeHandleCache;
  // Handles that don't have any more results to be fetched, they can be timed out aggressively.
  private final Cache<QueryHandle, InactiveOperationInfo> inactiveHandleCache;

  private final CLIService cliService;
  private final ScheduledExecutorService scheduledExecutorService;
  private final long cleanupJobSchedule;
  private final File previewsDir;

  protected abstract QueryStatus fetchStatus(OperationHandle handle) throws HiveSQLException, ExploreException,
    HandleNotFoundException;
  protected abstract OperationHandle doExecute(SessionHandle sessionHandle, String statement)
    throws HiveSQLException, ExploreException;

  protected BaseHiveExploreService(TransactionSystemClient txClient, DatasetFramework datasetFramework,
                                   CConfiguration cConf, Configuration hConf, HiveConf hiveConf, File previewsDir) {
    this.cConf = cConf;
    this.hConf = hConf;
    this.hiveConf = hiveConf;
    this.previewsDir = previewsDir;

    this.scheduledExecutorService =
      Executors.newSingleThreadScheduledExecutor(Threads.createDaemonThreadFactory("explore-handle-timeout"));

    this.activeHandleCache =
      CacheBuilder.newBuilder()
        .expireAfterWrite(cConf.getLong(Constants.Explore.ACTIVE_OPERATION_TIMEOUT_SECS), TimeUnit.SECONDS)
        .removalListener(new ActiveOperationRemovalHandler(this, scheduledExecutorService))
        .build();
    this.inactiveHandleCache =
      CacheBuilder.newBuilder()
        .expireAfterWrite(cConf.getLong(Constants.Explore.INACTIVE_OPERATION_TIMEOUT_SECS), TimeUnit.SECONDS)
        .build();

    this.cliService = new CLIService();

    this.txClient = txClient;
    ContextManager.saveContext(datasetFramework);

    cleanupJobSchedule = cConf.getLong(Constants.Explore.CLEANUP_JOB_SCHEDULE_SECS);

    LOG.info("Active handle timeout = {} secs", cConf.getLong(Constants.Explore.ACTIVE_OPERATION_TIMEOUT_SECS));
    LOG.info("Inactive handle timeout = {} secs", cConf.getLong(Constants.Explore.INACTIVE_OPERATION_TIMEOUT_SECS));
    LOG.info("Cleanup job schedule = {} secs", cleanupJobSchedule);
  }

  protected HiveConf getHiveConf() {
    // TODO figure out why this hive conf does not contain our env properties - REACTOR-270
    // return hiveConf;
    return new HiveConf();
  }

  protected CLIService getCliService() {
    return cliService;
  }

  @Override
  protected void startUp() throws Exception {
    LOG.info("Starting {}...", Hive13ExploreService.class.getSimpleName());
    cliService.init(getHiveConf());
    cliService.start();

    // Schedule the cache cleanup
    scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
                                                      @Override
                                                      public void run() {
                                                        runCacheCleanup();
                                                      }
                                                    }, cleanupJobSchedule, cleanupJobSchedule, TimeUnit.SECONDS
    );
  }

  @Override
  protected void shutDown() throws Exception {
    LOG.info("Stopping {}...", BaseHiveExploreService.class.getSimpleName());

    // By this time we should not get anymore new requests, since HTTP service has already been stopped.
    // Close all handles
    if (!activeHandleCache.asMap().isEmpty()) {
      LOG.info("Timing out active handles...");
    }
    activeHandleCache.invalidateAll();
    // Make sure the cache entries get expired.
    runCacheCleanup();

    // Wait for all cleanup jobs to complete
    scheduledExecutorService.awaitTermination(10, TimeUnit.SECONDS);
    scheduledExecutorService.shutdown();

    cliService.stop();
  }

  @Override
  public QueryHandle getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern)
    throws ExploreException, SQLException {
    try {
      Map<String, String> sessionConf = startSession();
      SessionHandle sessionHandle = cliService.openSession("", "", sessionConf);
      try {
        OperationHandle operationHandle = cliService.getColumns(sessionHandle, catalog, schemaPattern,
                                                                tableNamePattern, columnNamePattern);
        QueryHandle handle = saveOperationInfo(operationHandle, sessionHandle, sessionConf, "");
        LOG.trace("Retrieving columns: catalog {}, schemaPattern {}, tableNamePattern {}, columnNamePattern {}",
                  catalog, schemaPattern, tableNamePattern, columnNamePattern);
        return handle;
      } catch (Throwable e) {
        closeSession(sessionHandle);
        throw e;
      }
    } catch (HiveSQLException e) {
      throw getSqlException(e);
    } catch (Throwable e) {
      throw new ExploreException(e);
    }
  }

  @Override
  public QueryHandle getCatalogs() throws ExploreException, SQLException {
    try {
      Map<String, String> sessionConf = startSession();
      SessionHandle sessionHandle = cliService.openSession("", "", sessionConf);
      try {
        OperationHandle operationHandle = cliService.getCatalogs(sessionHandle);
        QueryHandle handle = saveOperationInfo(operationHandle, sessionHandle, sessionConf, "");
        LOG.trace("Retrieving catalogs");
        return handle;
      } catch (Throwable e) {
        closeSession(sessionHandle);
        throw e;
      }
    } catch (HiveSQLException e) {
      throw getSqlException(e);
    } catch (Throwable e) {
      throw new ExploreException(e);
    }
  }

  @Override
  public QueryHandle getSchemas(String catalog, String schemaPattern) throws ExploreException, SQLException {
    try {
      Map<String, String> sessionConf = startSession();
      SessionHandle sessionHandle = cliService.openSession("", "", sessionConf);
      try {
        OperationHandle operationHandle = cliService.getSchemas(sessionHandle, catalog, schemaPattern);
        QueryHandle handle = saveOperationInfo(operationHandle, sessionHandle, sessionConf, "");
        LOG.trace("Retrieving schemas: catalog {}, schema {}", catalog, schemaPattern);
        return handle;
      } catch (Throwable e) {
        closeSession(sessionHandle);
        throw e;
      }
    } catch (HiveSQLException e) {
      throw getSqlException(e);
    } catch (Throwable e) {
      throw new ExploreException(e);
    }
  }

  @Override
  public QueryHandle getFunctions(String catalog, String schemaPattern, String functionNamePattern)
    throws ExploreException, SQLException {
    try {
      Map<String, String> sessionConf = startSession();
      SessionHandle sessionHandle = cliService.openSession("", "", sessionConf);
      try {
        OperationHandle operationHandle = cliService.getFunctions(sessionHandle, catalog,
                                                                  schemaPattern, functionNamePattern);
        QueryHandle handle = saveOperationInfo(operationHandle, sessionHandle, sessionConf, "");
        LOG.trace("Retrieving functions: catalog {}, schema {}, function {}",
                  catalog, schemaPattern, functionNamePattern);
        return handle;
      } catch (Throwable e) {
        closeSession(sessionHandle);
        throw e;
      }
    } catch (HiveSQLException e) {
      throw getSqlException(e);
    } catch (Throwable e) {
      throw new ExploreException(e);
    }
  }

  @Override
  public MetaDataInfo getInfo(MetaDataInfo.InfoType infoType) throws ExploreException, SQLException {
    try {
      MetaDataInfo ret = infoType.getDefaultValue();
      if (ret != null) {
        return ret;
      }

      Map<String, String> sessionConf = startSession();
      SessionHandle sessionHandle = cliService.openSession("", "", sessionConf);
      try {
        // Convert to GetInfoType
        GetInfoType hiveInfoType = null;
        for (GetInfoType t : GetInfoType.values()) {
          if (t.name().equals("CLI_" + infoType.name())) {
            hiveInfoType = t;
            break;
          }
        }
        if (hiveInfoType == null) {
          // Should not come here, unless there is a mismatch between Explore and Hive info types.
          LOG.warn("Could not find Hive info type %s", infoType);
          return null;
        }
        GetInfoValue val = cliService.getInfo(sessionHandle, hiveInfoType);
        LOG.trace("Retrieving info: {}, got value {}", infoType, val);
        return new MetaDataInfo(val.getStringValue(), val.getShortValue(), val.getIntValue(), val.getLongValue());
      } finally {
        closeSession(sessionHandle);
      }
    } catch (HiveSQLException e) {
      throw getSqlException(e);
    } catch (IOException e) {
      throw new ExploreException(e);
    }
  }

  @Override
  public QueryHandle getTables(String catalog, String schemaPattern,
                          String tableNamePattern, List<String> tableTypes) throws ExploreException, SQLException {
    try {
      Map<String, String> sessionConf = startSession();
      SessionHandle sessionHandle = cliService.openSession("", "", sessionConf);
      try {
        OperationHandle operationHandle = cliService.getTables(sessionHandle, catalog, schemaPattern,
                                                               tableNamePattern, tableTypes);
        QueryHandle handle = saveOperationInfo(operationHandle, sessionHandle, sessionConf, "");
        LOG.trace("Retrieving tables: catalog {}, schemaNamePattern {}, tableNamePattern {}, tableTypes {}",
                  catalog, schemaPattern, tableNamePattern, tableTypes);
        return handle;
      } catch (Throwable e) {
        closeSession(sessionHandle);
        throw e;
      }
    } catch (HiveSQLException e) {
      throw getSqlException(e);
    } catch (Throwable e) {
      throw new ExploreException(e);
    }
  }

  @Override
  public QueryHandle getTableTypes() throws ExploreException, SQLException {
    try {
      Map<String, String> sessionConf = startSession();
      SessionHandle sessionHandle = cliService.openSession("", "", sessionConf);
      try {
        OperationHandle operationHandle = cliService.getTableTypes(sessionHandle);
        QueryHandle handle = saveOperationInfo(operationHandle, sessionHandle, sessionConf, "");
        LOG.trace("Retrieving table types");
        return handle;
      } catch (Throwable e) {
        closeSession(sessionHandle);
        throw e;
      }
    } catch (HiveSQLException e) {
      throw getSqlException(e);
    } catch (Throwable e) {
      throw new ExploreException(e);
    }
  }

  @Override
  public QueryHandle getTypeInfo() throws ExploreException, SQLException {
    try {
      Map<String, String> sessionConf = startSession();
      SessionHandle sessionHandle = cliService.openSession("", "", sessionConf);
      try {
        OperationHandle operationHandle = cliService.getTypeInfo(sessionHandle);
        QueryHandle handle = saveOperationInfo(operationHandle, sessionHandle, sessionConf, "");
        LOG.trace("Retrieving type info");
        return handle;
      } catch (Throwable e) {
        closeSession(sessionHandle);
        throw e;
      }
    } catch (HiveSQLException e) {
      throw getSqlException(e);
    } catch (Throwable e) {
      throw new ExploreException(e);
    }
  }

  @Override
  public QueryHandle execute(String statement) throws ExploreException, SQLException {
    try {
      Map<String, String> sessionConf = startSession();
      // TODO: allow changing of hive user and password - REACTOR-271
      // It looks like the username and password below is not used when security is disabled in Hive Server2.
      SessionHandle sessionHandle = cliService.openSession("", "", sessionConf);
      try {
        OperationHandle operationHandle = doExecute(sessionHandle, statement);
        QueryHandle handle = saveOperationInfo(operationHandle, sessionHandle, sessionConf, statement);
        LOG.trace("Executing statement: {} with handle {}", statement, handle);
        return handle;
      } catch (Throwable e) {
        closeSession(sessionHandle);
        throw e;
      }
    } catch (HiveSQLException e) {
      throw getSqlException(e);
    } catch (Throwable e) {
      throw new ExploreException(e);
    }
  }

  @Override
  public QueryStatus getStatus(QueryHandle handle) throws ExploreException, HandleNotFoundException, SQLException {
    InactiveOperationInfo inactiveOperationInfo = inactiveHandleCache.getIfPresent(handle);
    if (inactiveOperationInfo != null) {
      // Operation has been made inactive, so return the saved status.
      LOG.trace("Returning saved status for inactive handle {}", handle);
      return inactiveOperationInfo.getStatus();
    }

    try {
      // Fetch status from Hive
      QueryStatus status = fetchStatus(getOperationHandle(handle));
      LOG.trace("Status of handle {} is {}", handle, status);

      // No results or error, so can be timed out aggressively
      if (status.getStatus() == QueryStatus.OpStatus.FINISHED && !status.hasResults()) {
        timeoutAggresively(handle, getResultSchema(handle), status);
      } else if (status.getStatus() == QueryStatus.OpStatus.ERROR) {
        // getResultSchema will fail if the query is in error
        timeoutAggresively(handle, ImmutableList.<ColumnDesc>of(), status);
      }
      return status;
    } catch (HiveSQLException e) {
      throw getSqlException(e);
    }
  }

  @Override
  public List<QueryResult> nextResults(QueryHandle handle, int size)
    throws ExploreException, HandleNotFoundException, SQLException {
    InactiveOperationInfo inactiveOperationInfo = inactiveHandleCache.getIfPresent(handle);
    if (inactiveOperationInfo != null) {
      // Operation has been made inactive, so all results should have been fetched already - return empty list.
      LOG.trace("Returning empty result for inactive handle {}", handle);
      return ImmutableList.of();
    }

    try {
      // Fetch results from Hive
      LOG.trace("Getting results for handle {}", handle);
      List<QueryResult> results = fetchNextResults(getOperationHandle(handle), size);

      QueryStatus status = getStatus(handle);
      if (results.isEmpty() && status.getStatus() == QueryStatus.OpStatus.FINISHED) {
        // Since operation has fetched all the results, handle can be timed out aggressively.
        timeoutAggresively(handle, getResultSchema(handle), status);
      }
      return results;
    } catch (HiveSQLException e) {
      throw getSqlException(e);
    }
  }

  protected List<QueryResult> fetchNextResults(OperationHandle operationHandle, int size)
    throws HiveSQLException, ExploreException, HandleNotFoundException {
    try {
      if (operationHandle.hasResultSet()) {
        // Rowset is an interface in Hive 13, but a class in Hive 12, so we use reflection
        // so that the compiler does not make assumption on the return type of fetchResults
        Object rowSet = getCliService().fetchResults(operationHandle, FetchOrientation.FETCH_NEXT, size);
        Class rowSetClass = Class.forName("org.apache.hive.service.cli.RowSet");
        Method toTRowSetMethod = rowSetClass.getMethod("toTRowSet");
        TRowSet tRowSet = (TRowSet) toTRowSetMethod.invoke(rowSet);

        ImmutableList.Builder<QueryResult> rowsBuilder = ImmutableList.builder();
        for (TRow tRow : tRowSet.getRows()) {
          List<Object> cols = Lists.newArrayList();
          for (TColumnValue tColumnValue : tRow.getColVals()) {
            cols.add(tColumnToObject(tColumnValue));
          }
          rowsBuilder.add(new QueryResult(cols));
        }
        return rowsBuilder.build();
      } else {
        return Collections.emptyList();
      }
    } catch (ClassNotFoundException e) {
      throw Throwables.propagate(e);
    } catch (NoSuchMethodException e) {
      throw Throwables.propagate(e);
    } catch (InvocationTargetException e) {
      throw Throwables.propagate(e);
    } catch (IllegalAccessException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public List<QueryResult> previewResults(QueryHandle handle)
    throws ExploreException, HandleNotFoundException, SQLException {
    // TODO add synchronization to this thing?
    if (inactiveHandleCache.getIfPresent(handle) != null) {
      throw new HandleNotFoundException("Query is inactive.", true);
    }

    OperationInfo operationInfo = getOperationInfo(handle);
    File previewFile = operationInfo.getPreviewFile();
    if (previewFile != null) {
      try {
        Reader reader = new FileReader(previewFile);
        try {
          return GSON.fromJson(reader, new TypeToken<List<QueryResult>>() { }.getType());
        } finally {
          Closeables.closeQuietly(reader);
        }
      } catch (FileNotFoundException e) {
        LOG.error("Could not retrieve preview result file {}", previewFile, e);
        throw new ExploreException(e);
      }
    }

    FileWriter fileWriter = null;
    try {
      // Create preview results for query
      previewFile = new File(previewsDir, handle.getHandle());
      fileWriter = new FileWriter(previewFile);
      List<QueryResult> results = nextResults(handle, PREVIEW_COUNT);
      GSON.toJson(results, fileWriter);
      operationInfo.setPreviewFile(previewFile);
      return results;
    } catch (IOException e) {
      LOG.error("Could not write preview results into file", e);
      throw new ExploreException(e);
    } finally {
      if (fileWriter != null) {
        Closeables.closeQuietly(fileWriter);
      }
    }
  }

  @Override
  public List<ColumnDesc> getResultSchema(QueryHandle handle)
    throws ExploreException, HandleNotFoundException, SQLException {
    try {
      InactiveOperationInfo inactiveOperationInfo = inactiveHandleCache.getIfPresent(handle);
      if (inactiveOperationInfo != null) {
        // Operation has been made inactive, so return saved schema.
        LOG.trace("Returning saved schema for inactive handle {}", handle);
        return inactiveOperationInfo.getSchema();
      }

      // Fetch schema from hive
      LOG.trace("Getting schema for handle {}", handle);
      ImmutableList.Builder<ColumnDesc> listBuilder = ImmutableList.builder();
      OperationHandle operationHandle = getOperationHandle(handle);
      if (operationHandle.hasResultSet()) {
        TableSchema tableSchema = cliService.getResultSetMetadata(operationHandle);
        for (ColumnDescriptor colDesc : tableSchema.getColumnDescriptors()) {
          listBuilder.add(new ColumnDesc(colDesc.getName(), colDesc.getTypeName(),
                                         colDesc.getOrdinalPosition(), colDesc.getComment()));
        }
      }
      return listBuilder.build();
    } catch (HiveSQLException e) {
      throw getSqlException(e);
    }
  }

  /**
   * Cancel a running Hive operation. After the operation moves into a {@link QueryStatus.OpStatus#CANCELED},
   * {@link #close(QueryHandle)} needs to be called to release resources.
   *
   * @param handle handle returned by {@link #execute(String)}.
   * @throws ExploreException on any error cancelling operation.
   * @throws HandleNotFoundException when handle is not found.
   * @throws SQLException if there are errors in the SQL statement.
   */
  void cancelInternal(QueryHandle handle) throws ExploreException, HandleNotFoundException, SQLException {
    try {
      InactiveOperationInfo inactiveOperationInfo = inactiveHandleCache.getIfPresent(handle);
      if (inactiveOperationInfo != null) {
        // Operation has been made inactive, so no point in cancelling it.
        LOG.trace("Not running cancel for inactive handle {}", handle);
        return;
      }

      LOG.trace("Cancelling operation {}", handle);
      cliService.cancelOperation(getOperationHandle(handle));
    } catch (HiveSQLException e) {
      throw getSqlException(e);
    }
  }

  @Override
  public void close(QueryHandle handle) throws ExploreException, HandleNotFoundException {
    inactiveHandleCache.invalidate(handle);
    activeHandleCache.invalidate(handle);
  }

  @Override
  public List<QueryInfo> getQueries() throws ExploreException, SQLException {
    List<QueryInfo> result = Lists.newArrayList();
    for (Map.Entry<QueryHandle, OperationInfo> entry : activeHandleCache.asMap().entrySet()) {
      try {
        // we use empty query statement for get tables, get schemas, we don't need to return it this method call.
        if (!entry.getValue().getStatement().isEmpty()) {
          QueryStatus status = getStatus(entry.getKey());
          result.add(new QueryInfo(entry.getValue().getTimestamp(), entry.getValue().getStatement(),
                                   entry.getKey(), status, true));
        }
      } catch (HandleNotFoundException e) {
        // ignore the handle not found exception. this method returns all queries and handle, if the
        // handle is removed from the internal cache, then there is no point returning them from here.
      }
    }

    for (Map.Entry<QueryHandle, InactiveOperationInfo> entry : inactiveHandleCache.asMap().entrySet()) {
      try {
        // we use empty query statement for get tables, get schemas, we don't need to return it this method call.
        if (!entry.getValue().getStatement().isEmpty()) {
          QueryStatus status = getStatus(entry.getKey());
          result.add(new QueryInfo(entry.getValue().getTimestamp(),
                                   entry.getValue().getStatement(), entry.getKey(), status, false));
        }
      } catch (HandleNotFoundException e) {
        // ignore the handle not found exception. this method returns all queries and handle, if the
        // handle is removed from the internal cache, then there is no point returning them from here.
      }
    }
    Collections.sort(result);
    return result;
  }

  void closeInternal(QueryHandle handle, OperationInfo opInfo)
    throws ExploreException, HandleNotFoundException, SQLException {
    try {
      LOG.trace("Closing operation {}", handle);
      cliService.closeOperation(opInfo.getOperationHandle());
    } catch (HiveSQLException e) {
      throw getSqlException(e);
    } finally {
      try {
        closeSession(opInfo.getSessionHandle());
      } finally {
        cleanUp(handle, opInfo);
      }
    }
  }

  private void closeSession(SessionHandle sessionHandle) {
    try {
      cliService.closeSession(sessionHandle);
    } catch (Throwable e) {
      LOG.error("Got error closing session", e);
    }
  }

  /**
   * Starts a long running transaction, and also sets up session configuration.
   * @return configuration for a hive session that contains a transaction, and serialized CDAP configuration and
   * HBase configuration. This will be used by the map-reduce tasks started by Hive.
   * @throws IOException
   */
  protected Map<String, String> startSession() throws IOException {
    Map<String, String> sessionConf = Maps.newHashMap();

    Transaction tx = startTransaction();
    ConfigurationUtil.set(sessionConf, Constants.Explore.TX_QUERY_KEY, TxnCodec.INSTANCE, tx);
    ConfigurationUtil.set(sessionConf, Constants.Explore.CCONF_KEY, CConfCodec.INSTANCE, cConf);
    ConfigurationUtil.set(sessionConf, Constants.Explore.HCONF_KEY, HConfCodec.INSTANCE, hConf);
    return sessionConf;
  }

  /**
   * Returns {@link OperationHandle} associated with Explore {@link QueryHandle}.
   * @param handle explore handle.
   * @return OperationHandle.
   * @throws ExploreException
   */
  protected OperationHandle getOperationHandle(QueryHandle handle) throws ExploreException, HandleNotFoundException {
    return getOperationInfo(handle).getOperationHandle();
  }

  /**
   * Saves information associated with an Hive operation.
   * @param operationHandle {@link OperationHandle} of the Hive operation running.
   * @param sessionHandle {@link SessionHandle} for the Hive operation running.
   * @param sessionConf configuration for the session running the Hive operation.
   * @param statement SQL statement executed with the call.
   * @return {@link QueryHandle} that represents the Hive operation being run.
   */
  protected QueryHandle saveOperationInfo(OperationHandle operationHandle, SessionHandle sessionHandle,
                                     Map<String, String> sessionConf, String statement) {
    QueryHandle handle = QueryHandle.generate();
    activeHandleCache.put(handle, new OperationInfo(sessionHandle, operationHandle, sessionConf, statement));
    return handle;
  }

  /**
   * Called after a handle has been used to fetch all its results. This handle can be timed out aggressively.
   *
   * @param handle operation handle.
   */
  private void timeoutAggresively(QueryHandle handle, List<ColumnDesc> schema, QueryStatus status)
    throws HandleNotFoundException {
    OperationInfo opInfo = activeHandleCache.getIfPresent(handle);
    if (opInfo == null) {
      LOG.trace("Could not find OperationInfo for handle {}, it might already have been moved to inactive list",
                handle);
      return;
    }

    LOG.trace("Timing out handle {} aggressively", handle);
    inactiveHandleCache.put(handle, new InactiveOperationInfo(opInfo, schema, status));
    activeHandleCache.invalidate(handle);
  }

  private OperationInfo getOperationInfo(QueryHandle handle) throws HandleNotFoundException {
    // First look in running handles and handles that still can be fetched.
    OperationInfo opInfo = activeHandleCache.getIfPresent(handle);
    if (opInfo != null) {
      return opInfo;
    }
    throw new HandleNotFoundException("Invalid handle provided");
  }

  /**
   * Cleans up the metadata associated with active {@link QueryHandle}. It also closes associated transaction.
   * @param handle handle of the running Hive operation.
   */
  protected void cleanUp(QueryHandle handle, OperationInfo opInfo) {
    try {
      if (opInfo.getPreviewFile() != null) {
        opInfo.getPreviewFile().delete();
      }
      closeTransaction(handle, opInfo);
    } finally {
      activeHandleCache.invalidate(handle);
    }
  }

  private Transaction startTransaction() throws IOException {
    Transaction tx = txClient.startLong();
    LOG.trace("Transaction {} started.", tx);
    return tx;
  }

  private void closeTransaction(QueryHandle handle, OperationInfo opInfo) {
    try {
      Transaction tx = ConfigurationUtil.get(opInfo.getSessionConf(),
                                             Constants.Explore.TX_QUERY_KEY,
                                             TxnCodec.INSTANCE);
      LOG.trace("Closing transaction {} for handle {}", tx, handle);

      // Transaction doesn't involve any changes. We still commit it to take care of any side effect changes that
      // SplitReader may have.
      if (!(txClient.canCommit(tx, ImmutableList.<byte[]>of()) && txClient.commit(tx))) {
        txClient.abort(tx);
        LOG.info("Aborting transaction: {}", tx);
      }
    } catch (Throwable e) {
      LOG.error("Got exception while closing transaction.", e);
    }
  }

  private void runCacheCleanup() {
    LOG.trace("Running cache cleanup");
    activeHandleCache.cleanUp();
    inactiveHandleCache.cleanUp();
  }

  // Hive wraps all exceptions, including SQL exceptions in HiveSQLException. We would like to surface the SQL
  // exception to the user, and not other Hive server exceptions. We are using a heuristic to determine whether a
  // HiveSQLException is a SQL exception or not by inspecting the SQLState of HiveSQLException. If SQLState is not
  // null then we surface the SQL exception.
  private RuntimeException getSqlException(HiveSQLException e) throws ExploreException, SQLException {
    if (e.getSQLState() != null) {
      throw e;
    }
    throw new ExploreException(e);
  }

  protected Object tColumnToObject(TColumnValue tColumnValue) throws ExploreException {
    if (tColumnValue.isSetBoolVal()) {
      return tColumnValue.getBoolVal().isValue();
    } else if (tColumnValue.isSetByteVal()) {
      return tColumnValue.getByteVal().getValue();
    } else if (tColumnValue.isSetDoubleVal()) {
      return tColumnValue.getDoubleVal().getValue();
    } else if (tColumnValue.isSetI16Val()) {
      return tColumnValue.getI16Val().getValue();
    } else if (tColumnValue.isSetI32Val()) {
      return tColumnValue.getI32Val().getValue();
    } else if (tColumnValue.isSetI64Val()) {
      return tColumnValue.getI64Val().getValue();
    } else if (tColumnValue.isSetStringVal()) {
      return tColumnValue.getStringVal().getValue();
    }
    throw new ExploreException("Unknown column value encountered: " + tColumnValue);
  }

  /**
  * Helper class to store information about a Hive operation in progress.
  */
  static class OperationInfo {
    private final SessionHandle sessionHandle;
    private final OperationHandle operationHandle;
    private final Map<String, String> sessionConf;
    private final String statement;
    private final long timestamp;

    private File previewFile;

    OperationInfo(SessionHandle sessionHandle, OperationHandle operationHandle,
                  Map<String, String> sessionConf, String statement) {
      this.sessionHandle = sessionHandle;
      this.operationHandle = operationHandle;
      this.sessionConf = sessionConf;
      this.statement = statement;
      this.timestamp = System.currentTimeMillis();
      this.previewFile = null;
    }

    OperationInfo(SessionHandle sessionHandle, OperationHandle operationHandle,
                  Map<String, String> sessionConf, String statement, long timestamp) {
      this.sessionHandle = sessionHandle;
      this.operationHandle = operationHandle;
      this.sessionConf = sessionConf;
      this.statement = statement;
      this.timestamp = timestamp;
    }


    public SessionHandle getSessionHandle() {
      return sessionHandle;
    }

    public OperationHandle getOperationHandle() {
      return operationHandle;
    }

    public Map<String, String> getSessionConf() {
      return sessionConf;
    }

    public String getStatement() {
      return statement;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public File getPreviewFile() {
      return previewFile;
    }

    public void setPreviewFile(File previewFile) {
      this.previewFile = previewFile;
    }
  }

  private static class InactiveOperationInfo extends OperationInfo {
    private final List<ColumnDesc> schema;
    private final QueryStatus status;

    private InactiveOperationInfo(OperationInfo operationInfo, List<ColumnDesc> schema, QueryStatus status) {
      super(operationInfo.getSessionHandle(), operationInfo.getOperationHandle(),
            operationInfo.getSessionConf(), operationInfo.getStatement(), operationInfo.getTimestamp());
      this.schema = schema;
      this.status = status;
    }

    public List<ColumnDesc> getSchema() {
      return schema;
    }

    public QueryStatus getStatus() {
      return status;
    }
  }
}
