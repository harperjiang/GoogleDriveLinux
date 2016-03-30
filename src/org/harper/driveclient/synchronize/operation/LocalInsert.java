package org.harper.driveclient.synchronize.operation;

import java.io.File;
import java.io.IOException;

import org.harper.driveclient.Services;
import org.harper.driveclient.common.DriveUtils;
import org.harper.driveclient.common.StringUtils;
import org.harper.driveclient.snapshot.Snapshot;
import org.harper.driveclient.synchronize.Operation;

public class LocalInsert extends AbstractOperation {

	public LocalInsert(String localPath, String remoteFileId, Object... context) {
		super("LOCAL_INSERT", localPath, remoteFileId, context);
	}

	public LocalInsert(Operation another) {
		super("LOCAL_INSERT", another);
	}

	@Override
	public void execute() throws IOException {
		// Get remote id for the parent folder
		File local = DriveUtils.absolutePath(getLocalFile());
		String remoteParent = Services.getServices().storage().localToRemote()
				.get(DriveUtils.relativePath(local.getParentFile()));
		if (!StringUtils.isEmpty(remoteParent)
				&& !Services.getServices().storage().localToRemote()
						.containsKey(getLocalFile())) {
			// Ignore insert request that doesn't have a parent or
			// already have a mapping.
			// The insert operation will be accomplished by the topmost
			// folder
			String remoteId = Services.getServices().transmit()
					.upload(remoteParent, local);
			setRemoteFileId(remoteId);
		}
	}

	@Override
	public void updateSnapshot(Snapshot root) {

	}

}
