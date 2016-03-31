package org.harper.driveclient.synchronize.operation;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.harper.driveclient.Services;
import org.harper.driveclient.common.DriveUtils;
import org.harper.driveclient.common.StringUtils;
import org.harper.driveclient.snapshot.Snapshot;
import org.harper.driveclient.synchronize.Operation;

import com.google.api.services.drive.model.ParentReference;

public class RemoteInsert extends AbstractOperation {

	public RemoteInsert(String localPath, String remoteFileId,
			Object... context) {
		super("REMOTE_INSERT", localPath, remoteFileId, context);
	}

	public RemoteInsert(Operation another) {
		super("REMOTE_INSERT", another);
	}

	@Override
	public void execute() throws IOException {
		if (Services.getServices().storage().remoteToLocal()
				.containsKey(getRemoteFileId())) {
			// This file/folder already had been created
			return;
		}
		com.google.api.services.drive.model.File file = getContext(0);

		// Depth search of parent that has a local root
		List<ParentReference> path = new ArrayList<ParentReference>();
		File local = pathToLocal(getRemoteFileId(), path);
		if (null == local) {
			// Check whether this file is trashed
			if (file.getLabels().getTrashed()) {
				return;
			}
			if (file.getShared()) {
				return;
			} else {
				throw new IllegalArgumentException(
						"Non-trash file has no known parent");
			}
		}
		File parent = local;
		for (ParentReference node : path) {
			Services.getServices().transmit().download(node.getId(), parent);
			parent = DriveUtils.absolutePath(Services.getServices().storage()
					.remoteToLocal().get(node.getId()));
		}
		Services.getServices().transmit().download(getRemoteFileId(), parent);
		setLocalFile(Services.getServices().storage().remoteToLocal()
				.get(getRemoteFileId()));
		if (StringUtils.isEmpty(getLocalFile())) {
			return;
		}
	}

	@Override
	public void updateSnapshot(Snapshot root) {
		String localName = Services.getServices().storage().remoteToLocal()
				.get(getRemoteFileId());
		if (StringUtils.isEmpty(localName)) {
			// No local file, this remote insert is not caused by local
			// upload, nothing to do as Snapshot should have been updated in
			// LOCAL_INSERT
		} else {
			// Remote insert caused by local upload, only update snapshot
			File localFile = DriveUtils.absolutePath(localName);
			String parent = DriveUtils.relativePath(localFile.getParentFile());
			if (find(root, localName) != null)
				return;
			Snapshot parentsn = find(root, parent);

			Snapshot sn = Services.getServices().snapshot().make(localFile);
			parentsn.addChild(sn);
			parentsn.setDirty(true);
		}

	}

}
