package org.harper.driveclient.download;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.harper.driveclient.Constants;
import org.harper.driveclient.common.DefaultService;
import org.harper.driveclient.common.DriveUtils;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.util.IOUtils;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.model.File;

public class DefaultDownloadService extends DefaultService implements
		DownloadService {

	public DefaultDownloadService(Drive d) {
		super(d);
	}

	@Override
	public void download(String fileId, java.io.File localFolder)
			throws IOException {
		if (!(localFolder.exists() && localFolder.isDirectory())) {
			throw new IllegalArgumentException("Target is not directory");
		}
		File remoteFile = drive.files().get(fileId).execute();
		String path = MessageFormat.format("{0}{1}{2}",
				localFolder.getAbsolutePath(), java.io.File.separator,
				remoteFile.getTitle());
		if (DriveUtils.isDirectory(remoteFile)) {
			// Make local directory and download everything in it
			java.io.File newFolder = new java.io.File(path);
			if (!newFolder.mkdir()) {
				throw new RuntimeException("Cannot make directory");
			}
			List<ChildReference> children = drive.children().list(fileId)
					.execute().getItems();
			for (ChildReference child : children) {
				download(child.getId(), newFolder);
			}
		} else if (DriveUtils.isGoogleDoc(remoteFile)) {
			FileOutputStream fos = new FileOutputStream(path
					+ Constants.EXTENSION_GDOCS);
			fos.write(remoteFile.getDefaultOpenWithLink().getBytes());
			fos.close();
		} else {
			// Normal file
			String downloadUrl = remoteFile.getDownloadUrl();
			FileOutputStream fos = new FileOutputStream(path);
			downloadUrl(downloadUrl, fos);
			fos.close();
		}
	}

	protected void downloadUrl(String url, OutputStream writeTo)
			throws IOException {
		try {
			HttpResponse response = drive.getRequestFactory()
					.buildGetRequest(new GenericUrl(url)).execute();
			if (response.getStatusCode() == 200) {
				IOUtils.copy(response.getContent(), writeTo);
			} else {
				throw new RuntimeException("Server return "
						+ response.getStatusCode());
			}
		} catch (ClientProtocolException e) {
			throw new RuntimeException(e);
		}
	}
}
