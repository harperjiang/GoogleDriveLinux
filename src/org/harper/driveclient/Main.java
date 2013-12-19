package org.harper.driveclient;

import java.io.File;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.ChildReference;

public class Main {

	public static void main(String[] args) throws Exception {
		Drive drive = DriveClientFactory.createDrive();
		Services service = new Services(drive);

		String remoteRoot = Constants.FOLDER_ROOT;
		File localRoot = new File(Configuration.getLocalRoot());

		for (ChildReference childref : drive.children().list(remoteRoot)
				.execute().getItems()) {
			service.transmit().download(childref.getId(), localRoot);
		}
	}
}
