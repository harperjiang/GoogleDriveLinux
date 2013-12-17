package org.harper.driveclient.common;

import com.google.api.services.drive.Drive;

public class DefaultService {

	protected Drive drive;

	public DefaultService(Drive d) {
		this.drive = d;
	}
}
