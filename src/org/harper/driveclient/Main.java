package org.harper.driveclient;

import java.io.File;

import org.harper.driveclient.snapshot.Snapshot;

import com.google.api.services.drive.Drive;

public class Main {

	public static void main(String[] args) throws Exception {
		Drive drive = DriveClientFactory.createDrive();
		Services service = new Services(drive);

		String remoteRoot = "root";
		File localRoot = new File(Configuration.getLocalRoot());
		Snapshot snapshotRoot = service.snapshot().get();

		service.upload().upload("root", new File("./output/libs"));

		service.snapshot().make();
	}
}
