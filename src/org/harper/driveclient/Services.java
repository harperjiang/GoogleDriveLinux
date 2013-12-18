package org.harper.driveclient;

import org.harper.driveclient.change.ChangeService;
import org.harper.driveclient.change.DefaultChangeService;
import org.harper.driveclient.snapshot.DefaultSnapshotService;
import org.harper.driveclient.snapshot.SnapshotService;
import org.harper.driveclient.transmit.DefaultTransmitService;
import org.harper.driveclient.transmit.TransmitService;

import com.google.api.services.drive.Drive;

public class Services {

	private Drive drive;

	public Services(Drive drive) {
		this.drive = drive;
	}

	public ChangeService changes() {
		return new DefaultChangeService(drive);
	}

	public SnapshotService snapshot() {
		return new DefaultSnapshotService(drive);
	}

	public TransmitService transmit() {
		return new DefaultTransmitService(drive);
	}

}
