package org.harper.driveclient.synchronize.operation;

import java.io.File;
import java.text.MessageFormat;

import org.harper.driveclient.Services;
import org.harper.driveclient.common.DriveUtils;
import org.harper.driveclient.common.StringUtils;
import org.harper.driveclient.snapshot.Snapshot;
import org.harper.driveclient.synchronize.Operation;

public class RemoteDelete extends AbstractOperation {

	public RemoteDelete(String localPath, String remoteFileId,
			Object... context) {
		super("REMOTE_DELETE", localPath, remoteFileId, context);
	}

	public RemoteDelete(Operation another) {
		super("REMOTE_DELETE", another);
	}

	@Override
	public void execute() {
		if (StringUtils.isEmpty(getLocalFile())) {
			// No local file, remote file should be a trashed one.
			if (logger.isDebugEnabled()) {
				logger.debug("No corresponding local file for deletion. Remote file may be trashed");
			}
			return;
		}
		File localFile = DriveUtils.absolutePath(getLocalFile());
		String localParent = DriveUtils.relativePath(localFile.getParentFile());
		// Preserve the original context
		setContext(new Object[] { getContext(0), localParent });
		deleteLocalFile(localFile);
		if (localFile.exists()) {
			if (logger.isDebugEnabled()) {
				logger.debug(MessageFormat
						.format("Failed to delete file {0}. File may have been deleted.",
								getLocalFile()));
			}
		}
		Services.getServices().storage().localToRemote().remove(getLocalFile());
		Services.getServices().storage().remoteToLocal()
				.remove(getRemoteFileId());
	}

	@Override
	public void updateSnapshot(Snapshot root) {
		String fileName = getLocalFile();
		if (StringUtils.isEmpty(fileName)) {
			return;
		}
		Snapshot find = find(root, fileName);
		Snapshot parent = find.getParent();
		parent.getChildren().remove(find);
		parent.setDirty(true);
	}

}
