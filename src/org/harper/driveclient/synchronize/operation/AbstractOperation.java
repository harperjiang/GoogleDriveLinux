package org.harper.driveclient.synchronize.operation;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import org.harper.driveclient.Configuration;
import org.harper.driveclient.Services;
import org.harper.driveclient.common.DriveUtils;
import org.harper.driveclient.synchronize.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.drive.model.ParentReference;

public abstract class AbstractOperation implements Operation {

	private String name;

	private String localFile;

	private String remoteFileId;

	private Object[] context;

	protected Logger logger;

	public AbstractOperation(String name, String localPath,
			String remoteFileId, Object... context) {
		this.name = name;
		this.localFile = localPath;
		this.remoteFileId = remoteFileId;
		this.context = context;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	public AbstractOperation(String name, Operation another) {
		this(name, another.getLocalFile(), another.getRemoteFileId(), another
				.getContext());
	}

	public String getName() {
		return name;
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

	public <T> T getContext(int index) {
		return (T) context[index];
	}

	public void setContext(Object[] context) {
		this.context = context;
	}

	@Override
	public String toString() {
		return MessageFormat.format("{0}: local {1}, remote {2}", getName(),
				localFile, remoteFileId);
	}

	protected static File pathToLocal(String remoteFileId,
			List<ParentReference> path) throws IOException {
		List<ParentReference> pRefs = Services.getServices().getDrive()
				.parents().list(remoteFileId).execute().getItems();
		for (ParentReference pRef : pRefs) {
			if (pRef.getIsRoot()) {
				return Configuration.getLocalRoot();
			}
			if (Services.getServices().storage().remoteToLocal()
					.containsKey(pRef.getId())) {
				return DriveUtils.absolutePath(Services.getServices().storage()
						.remoteToLocal().get(pRef.getId()));
			} else {
				path.add(pRef);
				File lookInside = pathToLocal(pRef.getId(), path);
				if (null != lookInside) {
					return lookInside;
				} else {
					path.remove(pRef);
				}
			}
		}
		return null;
	}

	protected static void deleteLocalFile(File file) {
		if (!file.exists())
			return;
		// Execute a recursive delete
		if (file.isFile()) {
			file.delete();
		} else {
			for (File child : file.listFiles()) {
				deleteLocalFile(child);
			}
			file.delete();
		}
	}
}
