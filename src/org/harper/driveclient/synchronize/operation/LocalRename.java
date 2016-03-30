package org.harper.driveclient.synchronize.operation;

import java.io.IOException;

import org.harper.driveclient.Services;
import org.harper.driveclient.common.StringUtils;
import org.harper.driveclient.snapshot.Snapshot;
import org.harper.driveclient.synchronize.Operation;

public class LocalRename extends AbstractOperation {

	public LocalRename(String localPath, String remoteFileId, Object... context) {
		super("LOCAL_RENAME", localPath, remoteFileId, context);
	}

	public LocalRename(Operation another) {
		super("LOCAL_RENAME", another);
	}

	@Override
	public void execute() throws IOException {
		if (!StringUtils.isEmpty(getRemoteFileId())) {
			Services.getServices().transmit()
					.rename(getRemoteFileId(), (String) getContext()[0]);
		} else {
			logger.warn("Local rename has no remote reference, make sure the storage is correct.");
			logger.warn("Trying to insert the new record");
			// Modify it to be a local insert
			new LocalInsert(getLocalFile(), getRemoteFileId(), getContext())
					.execute();
		}
	}

	@Override
	public void updateSnapshot(Snapshot root) {
		// TODO Auto-generated method stub

	}

}
