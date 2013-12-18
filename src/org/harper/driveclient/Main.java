package org.harper.driveclient;

import java.io.File;

import com.google.api.services.drive.Drive;

public class Main {

	public static void main(String[] args) throws Exception {
		Drive drive = DriveClientFactory.createDrive();
		Services service = new Services(drive);

		String remoteRoot = Constants.FOLDER_ROOT;
		File localRoot = new File(Configuration.getLocalRoot());
		
		service.changes().remoteMd5();
		
		service.snapshot().make();
	}
}
