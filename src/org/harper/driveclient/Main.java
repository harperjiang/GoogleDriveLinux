package org.harper.driveclient;

import java.io.File;
import java.util.List;

import org.harper.driveclient.change.ChangeRecord;

import com.google.api.services.drive.Drive;

public class Main {

	public static void main(String[] args) throws Exception {
		Drive drive = DriveClientFactory.createDrive();

		String remoteRoot = "root";
		File localRoot = new File("./output");

		Services service = new Services(drive);

		List<ChangeRecord> changes = service.changes().compare(localRoot,
				remoteRoot);
		for (ChangeRecord change : changes) {
			change.synchronize();
		}
	}
}
