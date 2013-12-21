package org.harper.driveclient.snapshot;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import org.harper.driveclient.Configuration;
import org.harper.driveclient.Services;
import org.harper.driveclient.common.DefaultService;
import org.harper.driveclient.common.DriveUtils;

import com.google.api.services.drive.Drive;

public class DefaultSnapshotService extends DefaultService implements
		SnapshotService {

	public DefaultSnapshotService(Drive drive, Services stub) {
		super(drive, stub);
	}

	@Override
	public Snapshot get() {
		return null;
	}

	@Override
	public Snapshot make() throws IOException {
		java.io.File root = Configuration.getLocalRoot();
		return make(root);
	}

	protected Snapshot make(java.io.File root) {
		if (!root.exists())
			return null;
		Snapshot current = new Snapshot();
		current.setName(DriveUtils.relativePath(root));
		if (root.isDirectory()) {
			current.setFile(false);
			java.io.File[] children = root.listFiles();
			if (children != null) {
				for (java.io.File child : children) {
					current.addChild(make(child));
				}
			}
			// Sort children in alphabet order, and calculate the md5 of parent
			Map<String, String> md5s = new HashMap<String, String>();
			PriorityQueue<String> sort = new PriorityQueue<String>();
			for (Snapshot sc : current.getChildren()) {
				md5s.put(sc.getName(), sc.getMd5Checksum());
				sort.offer(sc.getName());
			}
			StringBuilder sb = new StringBuilder();
			while (!sort.isEmpty()) {
				sb.append(md5s.get(sort.poll()));
			}
			current.setMd5Checksum(DriveUtils.md5Checksum(sb.toString()));
		} else {
			current.setFile(true);
			current.setMd5Checksum(DriveUtils.md5Checksum(root));
		}
		return current;
	}

}
