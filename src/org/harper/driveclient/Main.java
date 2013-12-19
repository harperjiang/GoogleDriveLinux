package org.harper.driveclient;

import com.google.api.services.drive.Drive;

public class Main {

	public static void main(String[] args) throws Exception {
		Drive drive = DriveClientFactory.createDrive();
		Services service = new Services(drive);

		service.sync().synchronize();
	}
}
