package org.harper.driveclient;

import org.harper.driveclient.change.ChangeService;
import org.harper.driveclient.change.DefaultChangeService;
import org.harper.driveclient.download.DefaultDownloadService;
import org.harper.driveclient.download.DownloadService;
import org.harper.driveclient.snapshot.DefaultSnapshotService;
import org.harper.driveclient.snapshot.SnapshotService;
import org.harper.driveclient.upload.DefaultUploadService;
import org.harper.driveclient.upload.UploadService;

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

	public DownloadService download() {
		return new DefaultDownloadService(drive);
	}
	
	public UploadService upload() {
		return new DefaultUploadService(drive);
	}

}
