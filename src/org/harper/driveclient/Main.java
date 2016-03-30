package org.harper.driveclient;

import org.harper.driveclient.storage.StorageService;

import com.google.api.services.drive.Drive;

public class Main {

	public static void main(String[] args) throws Exception {
		Drive drive = DriveClientFactory.createDrive();
		Services service = Services.createServices(drive);

		while (true) {
			if (null == service.storage().get(StorageService.REMOTE_CHANGE)) {
				service.sync().init();
			} else {
				service.sync().synchronize();
			}
			Thread.sleep(Configuration.getCheckInterval());
		}
	}
}
