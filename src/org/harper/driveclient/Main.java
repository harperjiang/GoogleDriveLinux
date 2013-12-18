package org.harper.driveclient;

import java.io.File;
import java.util.List;

import org.harper.driveclient.snapshot.Snapshot;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.ChildReference;

public class Main {

	public static void main(String[] args) throws Exception {
		Drive drive = DriveClientFactory.createDrive();
		Services service = new Services(drive);

		String remoteRoot = "root";
		File localRoot = new File(Configuration.getLocalRoot());
		Snapshot snapshotRoot = service.snapshot().get();

		// Map<String,String> result = service.changes().remoteMd5("root");
		// List<ChangeRecord> changes = service.changes().compare(localRoot,
		// remoteRoot, snapshotRoot);
		// for (ChangeRecord change : changes) {
		// System.out.println(change.toString());
		// }
		List<ChildReference> children = drive.children().list("root").execute()
				.getItems();
		for (ChildReference child : children) {
			service.download().download(child.getId(), new File("./output"));
		}

		service.snapshot().make();
	}
}
