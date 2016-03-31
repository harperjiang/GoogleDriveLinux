package org.harper.driveclient.snapshot;

import java.io.IOException;
import java.text.MessageFormat;
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

	public Snapshot make(java.io.File root) {
		if (!root.exists())
			return null;
		Snapshot current = new Snapshot();
		current.make(root);
		return current;
	}
}
