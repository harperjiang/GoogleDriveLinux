package org.harper.driveclient.tools;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.harper.driveclient.common.StringUtils;
import org.harper.driveclient.snapshot.Snapshot;
import org.harper.driveclient.storage.DefaultStorageService;
import org.harper.driveclient.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StorageFix {

	static Logger logger = LoggerFactory.getLogger(StorageFix.class);

	public static void main(String[] args) {
		StorageService storage = new DefaultStorageService();
		// Check the invalid storage node

		logger.debug("Checking local to remote...");

		List<String> invalidKeys = new ArrayList<String>();
		for (Entry<String, String> lrEntry : storage.localToRemote().entrySet()) {
			if (StringUtils.isEmpty(lrEntry.getValue())) {
				logger.warn(MessageFormat.format(
						"{0} has no remote corresponding", lrEntry.getKey()));
				invalidKeys.add(lrEntry.getKey());
			}
		}
		if (invalidKeys.isEmpty()) {
			logger.debug("Local to remote checked. No error found.");
		} else {
			logger.debug("Removing invalid keys...");
			for (String key : invalidKeys)
				storage.localToRemote().remove(key);
			storage.persist();
		}
		logger.debug("Fixing snapshot...");
		Snapshot snapshot = storage.get(StorageService.SNAPSHOT);
		check(snapshot, storage.localToRemote());
		storage.persist();
		logger.debug("Finish fixing snapshot.");
		System.exit(0);
	}

	protected static void check(Snapshot snapshot,
			Map<String, String> localToRemote) {
		if (snapshot.isFile()) {
			return;
		}
		List<Snapshot> remove = new ArrayList<Snapshot>();
		for (Snapshot child : snapshot.getChildren()) {
			if (!localToRemote.containsKey(child.getName())
					|| StringUtils.isEmpty(localToRemote.get(child.getName()))) {
				logger.warn(MessageFormat
						.format("Snapshot node {0} has no remote corrresponding, will be removed",
								child.getName()));
				remove.add(child);
			}
		}
		for (Snapshot toRemove : remove) {
			localToRemote.remove(toRemove.getName());
			snapshot.getChildren().remove(toRemove);
		}
		for (Snapshot validChild : snapshot.getChildren()) {
			check(validChild, localToRemote);
		}
	}
}
