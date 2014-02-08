package org.harper.driveclient.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.harper.driveclient.Configuration;
import org.harper.driveclient.synchronize.FailedRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

public class DefaultStorageService implements StorageService {

	private Map<String, Object> storage;

	private ReadWriteLock lock;

	private boolean dirty;

	private Logger logger = LoggerFactory.getLogger(getClass());

	private Map<String, Parser> parsers;

	private File persistence = new File(Configuration.getConfigFolder()
			.getAbsolutePath() + File.separator + "storage");

	public DefaultStorageService() {
		storage = new HashMap<String, Object>();
		installParsers();
		lock = new ReentrantReadWriteLock();
		load();
		new PersistenceThread().start();
	}

	protected void installParsers() {
		parsers = new HashMap<String, Parser>();
		parsers.put(SNAPSHOT, new SnapshotParser());
		parsers.put(FAILED_LOG, new FailedRecordsParser());
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(String key) {
		lock.readLock().lock();
		try {
			return (T) storage.get(key);
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void put(String key, Object value) {
		lock.writeLock().lock();
		storage.put(key, value);
		dirty = true;
		lock.writeLock().unlock();
	}

	@SuppressWarnings("unchecked")
	protected void load() {
		lock.writeLock().lock();
		try {
			if (!persistence.exists())
				return;
			Reader jsonReader = new InputStreamReader(new FileInputStream(
					persistence));
			storage = new Gson().fromJson(jsonReader, Map.class);
			for (Entry<String, Object> entry : storage.entrySet()) {
				Parser parser = parsers.get(entry.getKey());
				if (null != parser) {
					storage.put(entry.getKey(), parser.parse(entry.getValue()));
				}
			}
		} catch (Exception e) {
			// Eat it
			logger.warn("Exception when loading storage", e);
		} finally {
			lock.writeLock().unlock();
		}
	}

	protected void store() {
		lock.readLock().lock();
		try {
			if (!dirty)
				return;
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			JsonWriter writer = new JsonWriter(new PrintWriter(
					new FileOutputStream(persistence)));
			gson.toJson(storage, Map.class, writer);
			writer.close();
			dirty = false;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			lock.readLock().unlock();
		}
	}

	public void persist() {
		// Force write
		this.dirty = true;
		store();
	}

	protected final class PersistenceThread extends Thread {
		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(60000);
					store();
				} catch (Exception e) {
				}
			}
		}
	}

	protected <T> T get(String key, T defaultValue) {
		if (null == get(key)) {
			synchronized (storage) {
				if (null == get(key)) {
					put(key, defaultValue);
				}
			}
		}
		return get(key);
	}

	@Override
	public Map<String, String> remoteToLocal() {
		return get(REMOTE_TO_LOCAL, new HashMap<String, String>());
	}

	@Override
	public Map<String, String> localToRemote() {
		return get(LOCAL_TO_REMOTE, new HashMap<String, String>());
	}

	@Override
	public List<FailedRecord> failedLog() {
		return get(FAILED_LOG, new ArrayList<FailedRecord>());
	}

	public static interface Parser {
		public Object parse(Object jsonInput);
	}
}
