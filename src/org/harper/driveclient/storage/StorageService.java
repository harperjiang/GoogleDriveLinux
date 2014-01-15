package org.harper.driveclient.storage;

import java.util.List;
import java.util.Map;

import org.harper.driveclient.synchronize.ChangeRecord;

public interface StorageService {

	public String SNAPSHOT = "snapshot";

	public String REMOTE_CHANGE = "remote_change";

	public String REMOTE_TO_LOCAL = "remote_to_local";

	public String LOCAL_TO_REMOTE = "local_to_remote";

	public String FAILED_LOG = "failed_log";

	public <T> T get(String key);

	public void put(String key, Object value);

	public Map<String, String> remoteToLocal();

	public Map<String, String> localToRemote();

	public List<ChangeRecord> failedLog();
}
