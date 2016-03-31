package org.harper.driveclient.synchronize.operation;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import org.harper.driveclient.Services;
import org.harper.driveclient.common.DriveUtils;
import org.harper.driveclient.snapshot.Snapshot;
import org.harper.driveclient.synchronize.Operation;

public class RemoteChange extends AbstractOperation {

	public RemoteChange(String localPath, String remoteFileId,
			Object... context) {
		super("REMOTE_CHANGE", localPath, remoteFileId, context);
	}

	public RemoteChange(Operation another) {
		super("REMOTE_CHANGE", another);
	}

	@Override
	public void execute() throws IOException {
		File local = DriveUtils.absolutePath(getLocalFile());
		File localParent = local.getParentFile();
		com.google.api.services.drive.model.File remote = getContext(0);

		if (!remote.getTitle().equals(local.getName())) {
			local.delete();
			Services.getServices().storage().localToRemote()
					.remove(getLocalFile());
			Services.getServices().storage().remoteToLocal()
					.remove(getRemoteFileId());
		}
		Services.getServices().transmit()
				.download(getRemoteFileId(), localParent);
	}

	@Override
	public void updateSnapshot(Snapshot root) {
		String fileName = getLocalFile();
		Snapshot old = find(root, fileName);
		com.google.api.services.drive.model.File remoteFile = getContext(0);
		old.setMd5Checksum(remoteFile.getMd5Checksum());
		old.setDirty(true);
	}

}