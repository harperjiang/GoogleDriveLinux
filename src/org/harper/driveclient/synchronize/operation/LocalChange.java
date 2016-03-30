package org.harper.driveclient.synchronize.operation;

import java.io.File;
import java.io.IOException;

import org.harper.driveclient.Services;
import org.harper.driveclient.common.DriveUtils;
import org.harper.driveclient.common.StringUtils;
import org.harper.driveclient.snapshot.Snapshot;
import org.harper.driveclient.synchronize.Operation;

public class LocalChange extends AbstractOperation {

	public LocalChange(String localPath, String remoteFileId, Object... context) {
		super("LOCAL_CHANGE", localPath, remoteFileId, context);
	}

	public LocalChange(Operation another) {
		super("LOCAL_CHANGE", another);
	}

	@Override
	public void execute() throws IOException {
		if (!StringUtils.isEmpty(getRemoteFileId())) {
			File local = DriveUtils.absolutePath(getLocalFile());
			Services.getServices().transmit().update(getRemoteFileId(), local);
		} else {
			logger.warn("Local change has no remote reference, make sure the storage is correct.");
			logger.warn("Trying to insert the new record");
			// Modify it to a local insert

			new LocalInsert(getLocalFile(), getRemoteFileId(), getContext())
					.execute();
		}
	}

	@Override
	public void updateSnapshot(Snapshot root) {
		// TODO Auto-generated method stub

	}

}
