package org.harper.driveclient.synchronize.operation;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import org.harper.driveclient.Services;
import org.harper.driveclient.common.DriveUtils;
import org.harper.driveclient.snapshot.Snapshot;
import org.harper.driveclient.synchronize.Operation;

public class RemoteRename extends AbstractOperation {

	public RemoteRename(String localPath, String remoteFileId,
			Object... context) {
		super("REMOTE_RENAME", localPath, remoteFileId, context);
	}

	public RemoteRename(Operation another) {
		super("REMOTE_RENAME", another);
	}

	@Override
	public void execute() throws IOException {
		File local = DriveUtils.absolutePath(getLocalFile());
		com.google.api.services.drive.model.File remote = getContext(0);
		String remoteName = remote.getTitle();
		File newName = new File(local.getParentFile().getAbsolutePath()
				+ File.separator + remoteName);
		String relNewName = DriveUtils.relativePath(newName);
		setContext(new Object[] { getContext()[0], relNewName });
		local.renameTo(newName);
		Services.getServices().storage().remoteToLocal()
				.put(getRemoteFileId(), DriveUtils.relativePath(newName));
		Services.getServices().storage().localToRemote()
				.remove(DriveUtils.relativePath(local));
		Services.getServices().storage().localToRemote()
				.put(DriveUtils.relativePath(newName), getRemoteFileId());
	}

	@Override
	public void updateSnapshot(Snapshot root) {
		String fileName = getLocalFile();

		if (root.getName().startsWith(fileName)) {
			String newName = getContext(1);
			root.setName(root.getName().replaceFirst(fileName, newName));
			for (Snapshot sn : root.getChildren()) {
				updateSnapshot(sn);
			}
		} else {
			for (Snapshot sn : root.getChildren()) {
				if (fileName.startsWith(sn.getName())) {
					updateSnapshot(sn);
					return;
				}
			}
			// Didn't find?
			logger.error(MessageFormat
					.format("Remote rename cannot find local corresponding {0}, should be an error",
							this));
			// throw new IllegalArgumentException();
		}
	}

}
