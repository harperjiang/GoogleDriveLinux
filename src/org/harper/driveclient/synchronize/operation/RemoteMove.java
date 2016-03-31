package org.harper.driveclient.synchronize.operation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.harper.driveclient.Services;
import org.harper.driveclient.common.DriveUtils;
import org.harper.driveclient.snapshot.Snapshot;
import org.harper.driveclient.synchronize.Operation;

public class RemoteMove extends AbstractOperation {

	public RemoteMove(String localPath, String remoteFileId, Object... context) {
		super("REMOTE_MOVE", localPath, remoteFileId, context);
	}

	public RemoteMove(Operation another) {
		super("REMOTE_MOVE", another);
	}

	private String oldName;

	private String newParent;

	private String newName;

	@Override
	public void execute() throws IOException {
		oldName = getLocalFile();
		File local = DriveUtils.absolutePath(getLocalFile());
		com.google.api.services.drive.model.File remote = getContext(0);

		String newParentId = remote.getParents().get(0).getId();
		if (Services.getServices().storage().remoteToLocal()
				.containsKey(newParentId)) {
			newParent = Services.getServices().storage().remoteToLocal()
					.get(newParentId);
			newName = newParent + File.separator + local.getName();
			Files.move(Paths.get(local.getName()), Paths.get(newName));
		} else {
			throw new RuntimeException(
					"Not found parent, wait for re-execution");
		}
	}

	@Override
	public void updateSnapshot(Snapshot root) {
		Snapshot find = find(root, oldName);
		if (null != find) {
			find.getParent().setDirty(true);
			find.getParent().getChildren().remove(find);
		}
		Snapshot newps = find(root, newParent);

		Snapshot item = new Snapshot();
		item.make(new File(newName));
		newps.addChild(item);
		newps.setDirty(true);
	}
}
