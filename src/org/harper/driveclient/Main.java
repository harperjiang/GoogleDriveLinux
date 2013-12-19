package org.harper.driveclient;

import java.io.File;

import com.google.api.services.drive.Drive;

public class Main {

	public static void main(String[] args) throws Exception {
		Drive drive = DriveClientFactory.createDrive();
		Services service = new Services(drive);

		String remoteRoot = Constants.FOLDER_ROOT;
		File localRoot = new File(Configuration.getLocalRoot());
		
		long start = System.currentTimeMillis();
		service.changes().remoteMd5();
		System.out.println(System.currentTimeMillis() - start);
		
		service.snapshot().make();
	}
}
