package org.harper.driveclient.synchronize.operation;

import java.io.IOException;

import org.harper.driveclient.Services;
import org.harper.driveclient.common.StringUtils;
import org.harper.driveclient.snapshot.Snapshot;
import org.harper.driveclient.synchronize.Operation;

public class LocalDelete extends AbstractOperation {

	public LocalDelete(String localPath, String remoteFileId, Object... context) {
		super("LOCAL_DELETE", localPath, remoteFileId, context);
	}

	public LocalDelete(Operation another) {
		super("LOCAL_DELETE", another);
	}

	@Override
	public void execute() throws IOException {
		if (!StringUtils.isEmpty(getRemoteFileId())) {
			Services.getServices().transmit().delete(getRemoteFileId());
		} else {
			logger.warn("Local deletion has no remote reference, make sure the storage is correct.");
		}
	}

	@Override
	public void updateSnapshot(Snapshot root) {
		// TODO Auto-generated method stub

	}

}
