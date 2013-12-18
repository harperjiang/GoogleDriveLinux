package org.harper.driveclient.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.drive.Drive;

public class DefaultService {

	protected Drive drive;
	
	protected Logger logger = LoggerFactory.getLogger(getClass());

	public DefaultService(Drive d) {
		this.drive = d;
	}
}
