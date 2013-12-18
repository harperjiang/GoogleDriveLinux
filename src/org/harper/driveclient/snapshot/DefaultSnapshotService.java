package org.harper.driveclient.snapshot;

import org.harper.driveclient.common.DefaultService;

import com.google.api.services.drive.Drive;

public class DefaultSnapshotService extends DefaultService implements SnapshotService {

	public DefaultSnapshotService(Drive drive) {
		super(drive);
	}

	@Override
	public Snapshot get() {
		return null;
	}

	@Override
	public Snapshot make() {
		// Create Snapshot from local directory
		
		
		return null;
	}

}
