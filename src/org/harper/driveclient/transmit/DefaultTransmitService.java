package org.harper.driveclient.transmit;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.harper.driveclient.Constants;
import org.harper.driveclient.Services;
import org.harper.driveclient.common.DefaultService;
import org.harper.driveclient.common.DriveUtils;
import org.harper.driveclient.common.Extension;
import org.harper.driveclient.common.MimeType;
import org.harper.driveclient.common.StringUtils;

import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.util.IOUtils;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;

public class DefaultTransmitService extends DefaultService implements
		TransmitService {

	public DefaultTransmitService(Drive d, Services stub) {
		super(d, stub);
	}

	protected static final String escape(String input) {
		return input;
	}

	@Override
	public void download(String fileId, java.io.File localFolder)
			throws IOException {
		if (!(localFolder.exists() && localFolder.isDirectory())) {
			throw new IllegalArgumentException("Target is not directory:"
					+ localFolder.getAbsolutePath());
		}
		File remoteFile = execute(drive.files().get(fileId));
		if (remoteFile.getLabels().getTrashed()) {
			logger.info("File is trashed:" + remoteFile.getTitle());
			return;
		}
		String path = MessageFormat.format("{0}{1}{2}",
				localFolder.getAbsolutePath(), java.io.File.separator,
				escape(remoteFile.getTitle()));
		if (DriveUtils.isDirectory(remoteFile)) {
			// Make local directory and download everything in it
			java.io.File newFolder = new java.io.File(path);
			if (!newFolder.mkdir()) {
				if (newFolder.isDirectory()) {
					if (logger.isDebugEnabled()) {
						logger.debug("Directory already exists:"
								+ newFolder.getAbsolutePath());
					}
				} else {
					throw new RuntimeException("Cannot make directory:"
							+ newFolder.getAbsolutePath());
				}
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("Creating directory:"
							+ newFolder.getAbsolutePath());
				}
			}
			List<ChildReference> children = execute(
					drive.children().list(fileId)).getItems();
			for (ChildReference child : children) {
				try {
					download(child.getId(), newFolder);
				} catch (Exception e) {
					// TODO Log the error, waiting for next synchronization
					logger.debug("Error occurred when downloading file:"
							+ child.getId(), e);
				}
			}
		} else if (DriveUtils.isGoogleDoc(remoteFile)) {
			// Ignore google doc files
			return;
		} else {
			// Normal file
			String downloadUrl = remoteFile.getDownloadUrl();
			FileOutputStream fos = new FileOutputStream(path);
			downloadUrl(downloadUrl, fos, 0);
			fos.close();
		}
		String relativePath = DriveUtils.relativePath(new java.io.File(path));
		stub.storage().remoteToLocal().put(fileId, relativePath);
		stub.storage().localToRemote().put(relativePath, fileId);
	}

	protected void downloadUrl(String url, OutputStream writeTo, long startPos)
			throws IOException {
		HttpRequest request = drive.getRequestFactory().buildGetRequest(
				new GenericUrl(url));
		if (startPos > 0) {
			// TODO Resumable download
		}
		long retryTime = 0;
		long counter = 1;
		while (true) {
			try {
				Thread.sleep(retryTime);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			HttpResponse response = request.execute();
			if (response.getStatusCode() == 200) {
				IOUtils.copy(response.getContent(), writeTo);
				break;
			} else {
				retryTime = (long) Math.pow(2, counter++) * 1000;
				if (logger.isDebugEnabled()) {
					logger.debug(MessageFormat.format(
							"Download file {0} return {1},"
									+ " waiting to retry after {2}", url,
							response.getStatusCode(), retryTime));
				}
			}
		}
	}

	@Override
	public String upload(String remoteFolder, java.io.File localFile)
			throws IOException {
		String remoteFileId = null;
		if (!localFile.exists()) {
			throw new IllegalArgumentException("Local file doesn't exist:"
					+ localFile.getAbsolutePath());
		}
		if (StringUtils.isEmpty(remoteFolder)) {
			throw new IllegalArgumentException("Remote folder doesn't exist");
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
			File inserted = execute(drive.files().insert(file));
			remoteFileId = inserted.getId();
			java.io.File[] localChildren = localFile.listFiles();
			if (localChildren != null) {
				for (java.io.File localChild : localChildren) {
					upload(inserted.getId(), localChild);
				}
			}
		} else {
			// TODO Here we didn't pay attention to google documents, as they
			// should not be created locally. All files being uploaded will
			// be assumed to be local files.

			// Determine the MIME type
			String extension = null;
			int extPoint = localFile.getName().lastIndexOf(".");
			if (extPoint != -1) {
				extension = localFile.getName().substring(extPoint + 1);
				Extension ext = Extension.match(extension);
				if (ext != null) {
					file.setMimeType(ext.getType().fullName());
				}
			}
			if (StringUtils.isEmpty(file.getMimeType())) {
				logger.warn(MessageFormat.format(
						"Unrecognized Extension:{0},use default mimetype",
						extension));
				file.setMimeType(MimeType.application_octet_stream.fullName());
			}
			File remoteFile = execute(drive.files().insert(file,
					new FileContent(file.getMimeType(), localFile)));
			remoteFileId = remoteFile.getId();
		}

		String relativePath = DriveUtils.relativePath(localFile);
		stub.storage().remoteToLocal().put(remoteFileId, relativePath);
		stub.storage().localToRemote().put(relativePath, remoteFileId);
		return remoteFileId;
	}

	@Override
	public void delete(String fileId) throws IOException {
		execute(drive.files().delete(fileId));
		String local = stub.storage().remoteToLocal().get(fileId);
		stub.storage().remoteToLocal().remove(fileId);
		stub.storage().localToRemote().remove(local);
	}

	@Override
	public void rename(String remoteId, String newName) throws IOException {
		File existing = execute(drive.files().get(remoteId));
		existing.setTitle(newName);
		execute(drive.files().update(remoteId, existing));
		String oldLocal = stub.storage().remoteToLocal().get(remoteId);
		String newLocal = DriveUtils.relativePath(new java.io.File(DriveUtils
				.absolutePath(oldLocal).getParent()
				+ java.io.File.separator
				+ newName));
		stub.storage().remoteToLocal().put(remoteId, newLocal);
		stub.storage().localToRemote().remove(oldLocal);
		stub.storage().localToRemote().put(newLocal, remoteId);
	}

	@Override
	public void update(String remoteId, java.io.File local) throws IOException {
		File existing = execute(drive.files().get(remoteId));
		execute(drive.files().update(remoteId, existing,
				new FileContent(existing.getMimeType(), local)));
	}
}
