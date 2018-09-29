package de.benjaminborbe.storage.service;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cassandra.thrift.NotFoundException;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.benjaminborbe.storage.api.StorageColumnIterator;
import de.benjaminborbe.storage.api.StorageException;
import de.benjaminborbe.storage.api.StorageIterator;
import de.benjaminborbe.storage.api.StorageRowIterator;
import de.benjaminborbe.storage.api.StorageService;
import de.benjaminborbe.storage.api.StorageValue;
import de.benjaminborbe.storage.util.StorageConfig;
import de.benjaminborbe.storage.util.StorageConnectionPool;
import de.benjaminborbe.storage.util.StorageDaoUtil;
import de.benjaminborbe.storage.util.StorageExporter;
import de.benjaminborbe.storage.util.StorageImporter;
import de.benjaminborbe.tools.util.Duration;
import de.benjaminborbe.tools.util.DurationUtil;

@Singleton
public class StorageServiceImpl implements StorageService {

	private static final long DURATION_WARN = 200;

	private final StorageConfig config;

	private final StorageDaoUtil storageDaoUtil;

	private final Logger logger;

	private final StorageConnectionPool storageConnectionPool;

	private final StorageExporter storageExporter;

	private final DurationUtil durationUtil;

	private final StorageImporter storageImporter;

	@Inject
	public StorageServiceImpl(
			final Logger logger,
			final StorageConfig config,
			final StorageDaoUtil storageDaoUtil,
			final StorageConnectionPool storageConnectionPool,
			final StorageExporter storageExporter,
			final StorageImporter storageImporter,
			final DurationUtil durationUtil) {
		this.logger = logger;
		this.config = config;
		this.storageDaoUtil = storageDaoUtil;
		this.storageConnectionPool = storageConnectionPool;
		this.storageExporter = storageExporter;
		this.storageImporter = storageImporter;
		this.durationUtil = durationUtil;
	}

	@Override
	public StorageValue get(final String columnFamily, final StorageValue id, final StorageValue key) throws StorageException {
		final Duration duration = durationUtil.getDuration();
		try {
			return storageDaoUtil.read(config.getKeySpace(), columnFamily, id, key);
		}
		catch (final NotFoundException e) {
			return null;
		}
		catch (final Exception e) {
			throw new StorageException(e);
		}
		finally {
			if (duration.getTime() > DURATION_WARN)
				logger.debug("duration " + duration.getTime());
		}
	}

	@Override
	public Map<StorageValue, StorageValue> get(final String columnFamily, final StorageValue id) throws StorageException {
		final Duration duration = durationUtil.getDuration();
		try {
			return storageDaoUtil.read(config.getKeySpace(), columnFamily, id);
		}
		catch (final NotFoundException e) {
			return null;
		}
		catch (final Exception e) {
			logger.trace("Exception", e);
			throw new StorageException(e);
		}
		finally {
			if (duration.getTime() > DURATION_WARN)
				logger.debug("duration " + duration.getTime());
		}
	}

	@Override
	public void delete(final String columnFamily, final StorageValue id, final StorageValue key) throws StorageException {
		final Duration duration = durationUtil.getDuration();
		try {
			delete(columnFamily, id, Arrays.asList(key));
		}
		finally {
			if (duration.getTime() > DURATION_WARN)
				logger.debug("duration " + duration.getTime());
		}
	}

	@Override
	public void set(final String columnFamily, final StorageValue id, final StorageValue key, final StorageValue value) throws StorageException {
		final Duration duration = durationUtil.getDuration();
		try {
			final Map<StorageValue, StorageValue> data = new HashMap<StorageValue, StorageValue>();
			data.put(key, value);
			set(columnFamily, id, data);
		}
		finally {
			if (duration.getTime() > DURATION_WARN)
				logger.debug("duration " + duration.getTime());
		}
	}

	@Override
	public void set(final String columnFamily, final StorageValue id, final Map<StorageValue, StorageValue> data) throws StorageException {
		final Duration duration = durationUtil.getDuration();
		try {
			storageDaoUtil.insert(config.getKeySpace(), columnFamily, id, data);
		}
		catch (final Exception e) {
			logger.trace(e.getClass().getSimpleName(), e);
			throw new StorageException(e);
		}
		finally {
			if (duration.getTime() > DURATION_WARN)
				logger.debug("duration " + duration.getTime());
		}
	}

	@Override
	public StorageIterator keyIterator(final String columnFamily) throws StorageException {
		final Duration duration = durationUtil.getDuration();
		try {
			return storageDaoUtil.keyIterator(config.getKeySpace(), columnFamily);
		}
		catch (final Exception e) {
			throw new StorageException(e);
		}
		finally {
			if (duration.getTime() > DURATION_WARN)
				logger.debug("duration " + duration.getTime());

		}
	}

	@Override
	public StorageIterator keyIterator(final String columnFamily, final Map<StorageValue, StorageValue> where) throws StorageException {
		final Duration duration = durationUtil.getDuration();
		try {
			return storageDaoUtil.keyIterator(config.getKeySpace(), columnFamily, where);
		}
		catch (final Exception e) {
			throw new StorageException(e);
		}
		finally {
			if (duration.getTime() > DURATION_WARN)
				logger.debug("duration " + duration.getTime());
		}
	}

	@Override
	public void delete(final String columnFamily, final StorageValue id, final Collection<StorageValue> keys) throws StorageException {
		final Duration duration = durationUtil.getDuration();
		try {
			for (final StorageValue key : keys) {
				try {
					storageDaoUtil.delete(config.getKeySpace(), columnFamily, id, key);
				}
				catch (final NotFoundException e) {
					// nop, already deleted
				}
			}
		}
		catch (final Exception e) {
			throw new StorageException(e);
		}
		finally {
			if (duration.getTime() > DURATION_WARN)
				logger.debug("duration " + duration.getTime());
		}
	}

	@Override
	public void delete(final String columnFamily, final StorageValue id) throws StorageException {
		final Duration duration = durationUtil.getDuration();
		try {
			storageDaoUtil.delete(config.getKeySpace(), columnFamily, id);
		}
		catch (final Exception e) {
			throw new StorageException(e);
		}
		finally {
			if (duration.getTime() > DURATION_WARN)
				logger.debug("duration " + duration.getTime());

		}
	}

	@Override
	public List<StorageValue> get(final String columnFamily, final StorageValue id, final List<StorageValue> keys) throws StorageException {
		final Duration duration = durationUtil.getDuration();
		try {
			return storageDaoUtil.read(config.getKeySpace(), columnFamily, id, keys);
		}
		catch (final NotFoundException e) {
			return null;
		}
		catch (final Exception e) {
			throw new StorageException(e);
		}
		finally {
			if (duration.getTime() > DURATION_WARN)
				logger.debug("duration " + duration.getTime());
		}
	}

	@Override
	public int getFreeConnections() {
		final Duration duration = durationUtil.getDuration();
		try {
			return storageConnectionPool.getFreeConnections();
		}
		finally {
			if (duration.getTime() > DURATION_WARN)
				logger.debug("duration " + duration.getTime());
		}
	}

	@Override
	public int getConnections() {
		final Duration duration = durationUtil.getDuration();
		try {
			return storageConnectionPool.getConnections();
		}
		finally {
			if (duration.getTime() > DURATION_WARN)
				logger.debug("duration " + duration.getTime());
		}
	}

	@Override
	public int getMaxConnections() {
		final Duration duration = durationUtil.getDuration();
		try {
			return storageConnectionPool.getMaxConnections();
		}
		finally {
			if (duration.getTime() > DURATION_WARN)
				logger.debug("duration " + duration.getTime());
		}
	}

	@Override
	public StorageRowIterator rowIterator(final String columnFamily, final List<StorageValue> columnNames) throws StorageException {
		final Duration duration = durationUtil.getDuration();
		try {
			return storageDaoUtil.rowIterator(config.getKeySpace(), columnFamily, columnNames);
		}
		catch (final Exception e) {
			throw new StorageException(e);
		}
		finally {
			if (duration.getTime() > DURATION_WARN)
				logger.debug("duration " + duration.getTime());
		}
	}

	@Override
	public StorageRowIterator rowIterator(final String columnFamily, final List<StorageValue> columnNames, final Map<StorageValue, StorageValue> where) throws StorageException {
		final Duration duration = durationUtil.getDuration();
		try {
			return storageDaoUtil.rowIterator(config.getKeySpace(), columnFamily, columnNames, where);
		}
		catch (final Exception e) {
			throw new StorageException(e);
		}
		finally {
			if (duration.getTime() > DURATION_WARN)
				logger.debug("duration " + duration.getTime());
		}
	}

	@Override
	public void backup() throws StorageException {
		final Duration duration = durationUtil.getDuration();
		try {
			final File targetDirectory = new File(config.getBackpuDirectory());
			storageExporter.export(targetDirectory, config.getKeySpace());
		}
		catch (final Exception e) {
			throw new StorageException(e);
		}
		finally {
			if (duration.getTime() > DURATION_WARN)
				logger.debug("duration " + duration.getTime());
		}
	}

	@Override
	public void restore(final String columnfamily, final String jsonContent) throws StorageException {
		final Duration duration = durationUtil.getDuration();
		try {
			logger.info("restore - columnfamily: " + columnfamily + " jsonContent: " + jsonContent);

			storageImporter.importJson(config.getKeySpace(), columnfamily, jsonContent);
		}
		catch (final Exception e) {
			throw new StorageException(e);
		}
		finally {
			if (duration.getTime() > DURATION_WARN)
				logger.debug("duration " + duration.getTime());
		}
	}

	@Override
	public long count(final String columnFamily) throws StorageException {
		final Duration duration = durationUtil.getDuration();
		try {
			return storageDaoUtil.count(config.getKeySpace(), columnFamily);
		}
		catch (final Exception e) {
			throw new StorageException(e);
		}
		finally {
			if (duration.getTime() > DURATION_WARN)
				logger.debug("duration " + duration.getTime());
		}
	}

	@Override
	public long count(final String columnFamily, final StorageValue columnName) throws StorageException {
		final Duration duration = durationUtil.getDuration();
		try {
			return storageDaoUtil.count(config.getKeySpace(), columnFamily, columnName);
		}
		catch (final Exception e) {
			throw new StorageException(e);
		}
		finally {
			if (duration.getTime() > DURATION_WARN)
				logger.debug("duration " + duration.getTime());
		}
	}

	@Override
	public long count(final String columnFamily, final StorageValue columnName, final StorageValue columnValue) throws StorageException {
		final Duration duration = durationUtil.getDuration();
		try {
			return storageDaoUtil.count(config.getKeySpace(), columnFamily, columnName, columnValue);
		}
		catch (final Exception e) {
			throw new StorageException(e);
		}
		finally {
			if (duration.getTime() > DURATION_WARN)
				logger.debug("duration " + duration.getTime());
		}
	}

	@Override
	public StorageColumnIterator columnIterator(final String columnFamily, final StorageValue key) throws StorageException {
		final Duration duration = durationUtil.getDuration();
		try {
			return storageDaoUtil.columnIterator(config.getKeySpace(), columnFamily, key);
		}
		catch (final Exception e) {
			throw new StorageException(e);
		}
		finally {
			if (duration.getTime() > DURATION_WARN)
				logger.debug("duration " + duration.getTime());
		}
	}

	@Override
	public String getEncoding() {
		return config.getEncoding();
	}

	@Override
	public Collection<List<StorageValue>> get(final String columnFamily, final Collection<StorageValue> keys, final List<StorageValue> columnNames) throws StorageException {
		final Duration duration = durationUtil.getDuration();
		try {
			return storageDaoUtil.read(config.getKeySpace(), columnFamily, keys, columnNames);
		}
		catch (final Exception e) {
			throw new StorageException(e);
		}
		finally {
			if (duration.getTime() > DURATION_WARN)
				logger.debug("duration " + duration.getTime());
		}
	}

}
