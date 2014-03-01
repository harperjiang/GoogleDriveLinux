package org.harper.driveclient.common;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.text.MessageFormat;

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

	public <T> T execute(DriveRequest<T> task) throws IOException {
		long counter = 0;
		long retryTime = 0;
		while (true) {
			try {
				Thread.sleep(retryTime);
				return task.execute();
			} catch (GoogleJsonResponseException e) {
				if (e.getDetails().getCode() == 404) {
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
				logger.error("Exception while executing drive task", e);
				throw e;
			} catch (InterruptedException e) {
				logger.error("Exception while executing drive task", e);
				throw new RuntimeException(e);
			}
		}
	}
}
