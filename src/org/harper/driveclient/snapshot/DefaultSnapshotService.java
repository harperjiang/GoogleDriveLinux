package org.harper.driveclient.snapshot;

import org.harper.driveclient.Services;
import org.harper.driveclient.common.DefaultService;

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
	public Snapshot make() {

		return null;
	}

}
