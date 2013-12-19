package org.harper.driveclient.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.harper.driveclient.Configuration;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonGenerator;
import com.google.api.client.json.JsonParser;
import com.google.api.client.json.jackson2.JacksonFactory;

public class DefaultStorageService implements StorageService {

	private Map<String, String> storage;

	private ReadWriteLock lock;

	private boolean dirty;

	private File persistence = new File(Configuration.getConfigFolder()
			.getAbsolutePath() + File.separator + "storage");

	public DefaultStorageService() {
		storage = new HashMap<String, String>();
		lock = new ReentrantReadWriteLock();
		load();
		new PersistenceThread().start();
	}

	@Override
	public String get(String key) {
		lock.readLock().lock();
		try {
			return storage.get(key);
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void put(String key, String value) {
		lock.writeLock().lock();
		storage.put(key, value);
		dirty = true;
		lock.writeLock().unlock();
	}

	protected void load() {
		lock.writeLock().lock();
		try {
			if (!persistence.exists())
				return;
			JsonParser parser = new JacksonFactory()
					.createJsonParser(new FileInputStream(persistence));
			storage = parser.parse(Map.class);
			parser.close();
		} catch (Exception e) {
			// Eat it
		} finally {
			lock.writeLock().unlock();
		}
	}

	protected void store() {
		lock.readLock().lock();
		try {
			if (!dirty)
				return;
			JsonFactory factory = new JacksonFactory();
			JsonGenerator generator = factory
					.createJsonGenerator(new PrintWriter(new FileOutputStream(
							persistence)));
			generator.serialize(storage);
			generator.close();
			dirty = false;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			lock.readLock().unlock();
		}
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
}
