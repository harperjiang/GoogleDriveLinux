package org.harper.driveclient.common;

import org.harper.driveclient.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.drive.Drive;

public class DefaultService {

	protected Drive drive;

	protected Services stub;

	protected Logger logger = LoggerFactory.getLogger(getClass());

	public DefaultService(Drive d, Services stub) {
		this.drive = d;
		this.stub = stub;
	}

	public Services getStub() {
		return stub;
	}

}
