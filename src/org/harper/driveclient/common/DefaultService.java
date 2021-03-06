package org.harper.driveclient.common;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.text.MessageFormat;

import org.harper.driveclient.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
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

	public <T> T execute(DriveRequest<T> task) throws IOException {
		long counter = 0;
		long retryTime = 0;
		while (true) {
			try {
				Thread.sleep(retryTime);
				if (task instanceof AbstractGoogleClientRequest) {
					// Reset the uploader when doing retry
					((AbstractGoogleClientRequest<T>) task).set("uploader",
							null);
				}
				return task.execute();
			} catch (GoogleJsonResponseException e) {
				if (e.getDetails().getCode() == 404) {
					throw e;
				}
				if (e.getDetails().getCode() == 403) {
					throw e;
				}
				retryTime = (long) Math.pow(2, counter++) * 1000;
				logger.warn(MessageFormat.format(
						"Exception while executing drive task, "
								+ "waiting to retry after {0}", retryTime), e);
			} catch (SocketTimeoutException e) {
				retryTime = (long) Math.pow(2, counter++) * 1000;
				logger.warn(MessageFormat.format(
						"Exception while executing drive task, "
								+ "waiting to retry after {0}", retryTime), e);
			} catch (IOException e) {
				retryTime = (long) Math.pow(2, counter++) * 1000;
				logger.error("Exception while executing drive task", e);
			} catch (InterruptedException e) {
				retryTime = (long) Math.pow(2, counter++) * 1000;
				logger.error("Exception while executing drive task", e);
			}
		}
	}
}
