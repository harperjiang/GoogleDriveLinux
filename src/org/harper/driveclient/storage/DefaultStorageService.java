package org.harper.driveclient.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.harper.driveclient.Configuration;

public class DefaultStorageService implements StorageService {

	private Map<String, Object> storage;

	private ReadWriteLock lock;

	private boolean dirty;

	private File persistence = new File(Configuration.getConfigFolder()
			.getAbsolutePath() + File.separator + "storage");

	public DefaultStorageService() {
		storage = new HashMap<String, Object>();
		lock = new ReentrantReadWriteLock();
		load();
		new PersistenceThread().start();
	}

	@Override
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

	protected void load() {
		lock.writeLock().lock();
		try {
			if (!persistence.exists())
				return;
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
					persistence));
			storage = (Map<String, Object>) ois.readObject();
			ois.close();
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
			ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream(persistence));
			oos.writeObject(storage);
			oos.close();
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

	@Override
	public Map<String, String> remoteToLocal() {
		return get(REMOTE_TO_LOCAL);
	}

	@Override
	public Map<String, String> localToRemote() {
		return get(LOCAL_TO_REMOTE);
	}
}
