package org.harper.driveclient;

import org.harper.driveclient.change.ChangeService;
import org.harper.driveclient.change.DefaultChangeService;

import com.google.api.services.drive.Drive;

public class Services {

	private Drive drive;

	public Services(Drive drive) {
		this.drive = drive;
	}

	public ChangeService changes() {
		return new DefaultChangeService(drive);
	}

}
