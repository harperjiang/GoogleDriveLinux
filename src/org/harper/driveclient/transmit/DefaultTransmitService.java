package org.harper.driveclient.transmit;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.harper.driveclient.Constants;
import org.harper.driveclient.common.DefaultService;
import org.harper.driveclient.common.DriveUtils;
import org.harper.driveclient.common.Extension;

import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.util.IOUtils;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;

public class DefaultTransmitService extends DefaultService implements
		TransmitService {

	public DefaultTransmitService(Drive d) {
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

	@Override
	public void upload(String remoteFolder, java.io.File localFile)
			throws IOException {
		if (!localFile.exists()) {
			throw new IllegalArgumentException("Local file doesn't exist:"
					+ localFile.getAbsolutePath());
		}

		File file = new File();
		file.setTitle(localFile.getName());
		// Set Parent
		List<ParentReference> parents = new ArrayList<ParentReference>();
		ParentReference parent = new ParentReference();
		parent.setId(remoteFolder);
		parents.add(parent);
		file.setParents(parents);
		if (localFile.isDirectory()) {
			file.setMimeType(Constants.TYPE_FOLDER);
			// Execute
			File inserted = drive.files().insert(file).execute();
			ChildReference child = new ChildReference();
			child.setId(inserted.getId());
			drive.children().insert(remoteFolder, child).execute();
			java.io.File[] localChildren = localFile.listFiles();
			if (localChildren != null) {
				for (java.io.File localChild : localChildren) {
					upload(inserted.getId(), localChild);
				}
			}
		} else {
			// TODO Here we didn't pay attention to google documents, as it
			// should not be created locally. Thus all files being uploaded will
			// be assumed to be local files.

			// Determine the MIME type
			String extension = null;
			int extPoint = localFile.getName().lastIndexOf(".");
			if (extPoint != -1) {
				extension = localFile.getName().substring(extPoint + 1);
				Extension ext = Extension.match(extension);
				if (ext != null)
					file.setMimeType(ext.getType().fullName());
				else {
					logger.warn("Unrecognized Extension:" + extension);
				}
			}
			drive.files()
					.insert(file,
							new FileContent(file.getMimeType(), localFile))
					.execute();
		}
	}
}
