package org.harper.driveclient;

import org.harper.driveclient.storage.StorageService;

import com.google.api.services.drive.Drive;

public class Main {

	public static void main(String[] args) throws Exception {
		Drive drive = DriveClientFactory.createDrive();
		Services service = new Services(drive);
		if (null == service.storage().get(StorageService.REMOTE_CHANGE)) {
			service.sync().init();
		} else {
			service.sync().synchronize();
		}
	}
}
