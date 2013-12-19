package org.harper.driveclient;

import org.harper.driveclient.common.StringUtils;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Changes.List;
import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.ChangeList;

public class Main {

	public static void main(String[] args) throws Exception {
		Drive drive = DriveClientFactory.createDrive();
		Services service = new Services(drive);

		System.out.println(service.sync().l)
	}
}
