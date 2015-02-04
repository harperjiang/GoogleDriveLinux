package org.harper.driveclient.tools;

import org.harper.driveclient.snapshot.Snapshot;
import org.harper.driveclient.storage.DefaultStorageService;
import org.harper.driveclient.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListStorage {

	static Logger logger = LoggerFactory.getLogger(StorageFix.class);

	public static void main(String[] args) {
		StorageService storage = new DefaultStorageService();
		// Check the invalid storage node

		logger.debug("Listing snapshots...");
		Snapshot snapshot = storage.get(StorageService.SNAPSHOT);
		printSnapshot(snapshot, 0);
		System.exit(0);
	}

	private static void printSnapshot(Snapshot root, int level) {
		for (int i = 0; i < 2 * level; i++)
			System.out.print(" ");
		System.out.println(root.getName());
		for (Snapshot child : root.getChildren())
			printSnapshot(child, level + 1);
	}
}
