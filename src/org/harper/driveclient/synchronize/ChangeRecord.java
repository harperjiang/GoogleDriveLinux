package org.harper.driveclient.synchronize;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.harper.driveclient.Services;
import org.harper.driveclient.common.DriveUtils;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.ParentReference;

public class ChangeRecord {

	public static enum Operation {
		LOCAL_INSERT, LOCAL_DELETE, LOCAL_CHANGE, LOCAL_RENAME, REMOTE_INSERT, REMOTE_DELETE, REMOTE_CHANGE, REMOTE_RENAME
	}

	private Operation operation;

	private String localFile;

	private String remoteFileId;

	private Object[] context;

	public ChangeRecord(Operation operation, String localPath,
			String remoteFileId, Object... context) {
		this.operation = operation;
		this.localFile = localPath;
		this.remoteFileId = remoteFileId;
		this.context = context;
	}

	public void synchronize(Drive drive, Services service) throws IOException {
		switch (operation) {
		case LOCAL_INSERT: {
			// Get remote id for the parent folder
			File local = DriveUtils.absolutePath(localFile);
			String remoteParent = service.storage().localToRemote()
					.get(DriveUtils.relativePath(local.getParentFile()));
			service.transmit().upload(remoteParent, local);
			break;
		}
		case LOCAL_DELETE: {
			service.transmit().delete(remoteFileId);
			break;
		}
		case LOCAL_CHANGE: {
			File local = DriveUtils.absolutePath(localFile);
			service.transmit().update(remoteFileId, local);
			break;
		}
		case LOCAL_RENAME: {
			service.transmit().rename(remoteFileId, (String) context[0]);
			break;
		}
		case REMOTE_INSERT: {
			// Query Remote parent
			List<ParentReference> parents = drive.parents().list(remoteFileId)
					.execute().getItems();
			String parentId = parents.get(0).getId();
			File localParent = DriveUtils.absolutePath(service.storage()
					.remoteToLocal().get(parentId));
			service.transmit().download(remoteFileId, localParent);
			break;
		}
		case REMOTE_DELETE: {
			DriveUtils.absolutePath(localFile).delete();
			service.storage().localToRemote().remove(localFile);
			service.storage().remoteToLocal().remove(remoteFileId);
			break;
		}
		case REMOTE_CHANGE: {
			File localParent = DriveUtils.absolutePath(localFile)
					.getParentFile();
			service.transmit().download(remoteFileId, localParent);
			break;
		}
		case REMOTE_RENAME: {
			File local = DriveUtils.absolutePath(localFile);
			File newName = new File(local.getParentFile().getAbsolutePath()
					+ File.separator + (String) context[0]);
			local.renameTo(newName);
			service.storage().remoteToLocal()
					.put(remoteFileId, DriveUtils.relativePath(newName));
			service.storage().localToRemote()
					.remove(DriveUtils.relativePath(local));
			service.storage().localToRemote()
					.put(DriveUtils.relativePath(newName), remoteFileId);
			break;
		}
		}

	}

	public Operation getOperation() {
		return operation;
	}

	public void setOperation(Operation operation) {
		this.operation = operation;
	}

	public String getLocalFile() {
		return localFile;
	}

	public void setLocalFile(String localFile) {
		this.localFile = localFile;
	}

	public String getRemoteFileId() {
		return remoteFileId;
	}

	public void setRemoteFileId(String remoteFileId) {
		this.remoteFileId = remoteFileId;
	}

	public Object[] getContext() {
		return context;
	}

	public void setContext(Object[] context) {
		this.context = context;
	}

}
