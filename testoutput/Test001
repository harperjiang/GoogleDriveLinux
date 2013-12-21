package org.harper.driveclient;

import org.harper.driveclient.snapshot.DefaultSnapshotService;
import org.harper.driveclient.snapshot.SnapshotService;
import org.harper.driveclient.storage.DefaultStorageService;
import org.harper.driveclient.storage.StorageService;
import org.harper.driveclient.synchronize.DefaultSynchronizeService;
import org.harper.driveclient.synchronize.SynchronizeService;
import org.harper.driveclient.transmit.DefaultTransmitService;
import org.harper.driveclient.transmit.TransmitService;

import com.google.api.services.drive.Drive;

public class Services {
	
	private Drive drive;

	public Services(Drive drive) {
		this.drive = drive;
	}

	public SynchronizeService sync() {
		return new DefaultSynchronizeService(drive, this);
	}

	public SnapshotService snapshot() {
		return new DefaultSnapshotService(drive, this);
	}

	public TransmitService transmit() {
		return new DefaultTransmitService(drive, this);
	}

	private StorageService storageService = new DefaultStorageService();

	public StorageService storage() {
		return storageService;
	}
}
