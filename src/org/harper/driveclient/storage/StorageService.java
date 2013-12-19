package org.harper.driveclient.storage;

public interface StorageService {

	public String get(String key);

	public void put(String key, String value);
}
