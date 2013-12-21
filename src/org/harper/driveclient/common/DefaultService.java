package org.harper.driveclient.common;

import java.io.IOException;

import org.harper.driveclient.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveRequest;

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

	public <T> T execute(DriveRequest<T> task) {
		long interval = 0;
		while (true) {
			try {
				Thread.sleep(((long) (Math.pow(2, interval) - 1) * 1000));
				return task.execute();
			} catch (GoogleJsonResponseException e) {
				logger.warn(
						"Exception while executing drive task, waiting to retry",
						e);
				interval++;
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
