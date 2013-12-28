package org.harper.driveclient.synchronize;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections4.CollectionUtils;
import org.harper.driveclient.Configuration;
import org.harper.driveclient.Constants;
import org.harper.driveclient.Services;
import org.harper.driveclient.common.DefaultService;
import org.harper.driveclient.common.DriveUtils;
import org.harper.driveclient.common.StringUtils;
import org.harper.driveclient.snapshot.Snapshot;
import org.harper.driveclient.storage.StorageService;
import org.harper.driveclient.synchronize.ChangeRecord.Operation;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.ChangeList;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.model.ParentReference;

public class DefaultSynchronizeService extends DefaultService implements
		SynchronizeService {

	public DefaultSynchronizeService(Drive d, Services stub) {
		super(d, stub);
	}

	@Override
	public void init() throws IOException {
		// Detect and store change number
		long latestChange = changes(null);
		if (latestChange != -1)
			stub.storage().put(StorageService.REMOTE_CHANGE, latestChange);

		// Download files
		stub.storage().put(StorageService.REMOTE_TO_LOCAL,
				new HashMap<String, String>());
		stub.storage().put(StorageService.LOCAL_TO_REMOTE,
				new HashMap<String, String>());
		String remoteRoot = Constants.FOLDER_ROOT;
		java.io.File localRoot = Configuration.getLocalRoot();
		String localRelative = DriveUtils.relativePath(localRoot);
		stub.storage().remoteToLocal().put(remoteRoot, localRelative);
		stub.storage().localToRemote().put(localRelative, remoteRoot);

		for (ChildReference childref : drive.children().list(remoteRoot)
				.execute().getItems()) {
			stub.transmit().download(childref.getId(), localRoot);
		}

		// Make local snapshot;
		stub.storage().put(StorageService.SNAPSHOT, stub.snapshot().make());
	}

	@Override
	public void synchronize() throws IOException {
		// Detect remote change
		List<ChangeRecord> remoteChanges = remoteChange();
		// Detect local change
		Snapshot standard = stub.storage().get(StorageService.SNAPSHOT);
		Snapshot current = stub.snapshot().make();
		List<ChangeRecord> localChanges = compare(standard, current);

		/*
		 * First upload then download. Local change has higher priority than
		 * remote because remote change can be restored while local cannot.
		 * Recorded uploaded file so later no need to download
		 */

		Map<String, String> localUploaded = new HashMap<String, String>();
		for (ChangeRecord localChange : localChanges) {
			synchronize(localChange);
			localUploaded.put(localChange.getLocalFile(),
					localChange.getRemoteFileId());
		}

		for (ChangeRecord remoteChange : remoteChanges) {
			if (!localUploaded.containsKey(remoteChange.getLocalFile())) {
				synchronize(remoteChange);
			}
		}

		// Save the new snapshot
		stub.storage().put(StorageService.SNAPSHOT, stub.snapshot().make());
		// Save the new change id
		stub.storage().put(StorageService.REMOTE_CHANGE, changes(null));
	}

	private void synchronize(ChangeRecord record) throws IOException {
		switch (record.getOperation()) {
		case LOCAL_INSERT: {
			// Get remote id for the parent folder
			File local = DriveUtils.absolutePath(record.getLocalFile());
			String remoteParent = stub.storage().localToRemote()
					.get(DriveUtils.relativePath(local.getParentFile()));
			stub.transmit().upload(remoteParent, local);
			break;
		}
		case LOCAL_DELETE: {
			stub.transmit().delete(record.getRemoteFileId());
			break;
		}
		case LOCAL_CHANGE: {
			File local = DriveUtils.absolutePath(record.getLocalFile());
			stub.transmit().update(record.getRemoteFileId(), local);
			break;
		}
		case LOCAL_RENAME: {
			stub.transmit().rename(record.getRemoteFileId(),
					(String) record.getContext()[0]);
			break;
		}
		case REMOTE_INSERT: {
			// Query Remote parent
			List<ParentReference> parents = drive.parents()
					.list(record.getRemoteFileId()).execute().getItems();
			String parentId = parents.get(0).getId();
			File localParent = DriveUtils.absolutePath(stub.storage()
					.remoteToLocal().get(parentId));
			stub.transmit().download(record.getRemoteFileId(), localParent);
			break;
		}
		case REMOTE_DELETE: {
			DriveUtils.absolutePath(record.getLocalFile()).delete();
			stub.storage().localToRemote().remove(record.getLocalFile());
			stub.storage().remoteToLocal().remove(record.getRemoteFileId());
			break;
		}
		case REMOTE_CHANGE: {
			File local = DriveUtils.absolutePath(record.getLocalFile());
			File localParent = local.getParentFile();
			if (!execute(drive.files().get(record.getRemoteFileId()))
					.getTitle().equals(local.getName())) {
				local.delete();
				stub.storage().localToRemote().remove(record.getLocalFile());
				stub.storage().remoteToLocal().remove(record.getRemoteFileId());
			}
			stub.transmit().download(record.getRemoteFileId(), localParent);
			break;
		}
		case REMOTE_RENAME: {
			File local = DriveUtils.absolutePath(record.getLocalFile());
			File newName = new File(local.getParentFile().getAbsolutePath()
					+ File.separator + (String) record.getContext()[0]);
			local.renameTo(newName);
			stub.storage()
					.remoteToLocal()
					.put(record.getRemoteFileId(),
							DriveUtils.relativePath(newName));
			stub.storage().localToRemote()
					.remove(DriveUtils.relativePath(local));
			stub.storage()
					.localToRemote()
					.put(DriveUtils.relativePath(newName),
							record.getRemoteFileId());
			break;
		}
		}

	}

	private List<ChangeRecord> remoteChange() throws IOException {
		List<Change> changes = new ArrayList<Change>();
		changes(changes);

		List<ChangeRecord> records = new ArrayList<ChangeRecord>();

		for (Change remoteChange : changes) {
			String local = stub.storage().remoteToLocal()
					.get(remoteChange.getFileId());
			if (StringUtils.isEmpty(local)) {
				// Cannot find this file, should be new
				records.add(new ChangeRecord(Operation.REMOTE_INSERT, null,
						remoteChange.getFileId()));
			} else if (remoteChange.getDeleted()) {
				records.add(new ChangeRecord(Operation.REMOTE_DELETE, local,
						remoteChange.getFileId()));
			} else {
				if (!DriveUtils.isDirectory(remoteChange.getFile())) {
					String remoteName = remoteChange.getFile().getTitle();
					String remoteMd5 = remoteChange.getFile().getMd5Checksum();
					String localMd5 = DriveUtils.md5Checksum(DriveUtils
							.absolutePath(local));
					if (localMd5.equals(remoteMd5)) {
						records.add(new ChangeRecord(Operation.REMOTE_RENAME,
								local, remoteChange.getFileId(), remoteName));
					} else {
						records.add(new ChangeRecord(Operation.REMOTE_CHANGE,
								local, remoteChange.getFileId()));
					}
				} else {
					// For directory just check the name change
					String remoteName = remoteChange.getFile().getTitle();
					String localName = DriveUtils.absolutePath(local).getName();
					if (!remoteName.equals(localName)) {
						records.add(new ChangeRecord(Operation.REMOTE_RENAME,
								local, remoteChange.getFileId(), remoteName));
					}
				}
			}
		}

		return records;
	}

	protected List<ChangeRecord> compare(Snapshot standard, Snapshot current) {
		List<ChangeRecord> changes = new ArrayList<ChangeRecord>();

		if (standard.isFile() != current.isFile()) {
			throw new IllegalArgumentException(
					"Comparison between file and folder:" + standard.getName()
							+ ":" + current.getName());
		}
		// The same
		if (standard.getMd5Checksum().equals(current.getMd5Checksum())) {
			return changes;
		}

		if (standard.isFile()) {
			changes.add(new ChangeRecord(Operation.LOCAL_CHANGE, standard
					.getName(), stub.storage().localToRemote()
					.get(standard.getName())));
		} else {
			Map<String, Snapshot> scMd5Table = new HashMap<String, Snapshot>();
			Map<String, Snapshot> scNameTable = new HashMap<String, Snapshot>();
			Map<String, Snapshot> ccMd5Table = new HashMap<String, Snapshot>();
			Map<String, Snapshot> ccNameTable = new HashMap<String, Snapshot>();
			for (Snapshot sc : standard.getChildren()) {
				scMd5Table.put(sc.getMd5Checksum(), sc);
				scNameTable.put(sc.getName(), sc);
			}
			for (Snapshot cc : current.getChildren()) {
				ccMd5Table.put(cc.getMd5Checksum(), cc);
				ccNameTable.put(cc.getName(), cc);
			}

			// same name, same md5 => unchange
			// same name, diff md5, same type => change
			// same name, diff md5, diff type => delete and insert
			// diff name, same md5 => rename
			// other => new or delete

			// Check file with same names
			Collection<String> sameName = CollectionUtils.intersection(
					scNameTable.keySet(), ccNameTable.keySet());

			for (String name : sameName) {
				Snapshot oldrec = scNameTable.remove(name);
				Snapshot newrec = ccNameTable.remove(name);
				if (oldrec.isFile() != newrec.isFile()) {
					changes.add(new ChangeRecord(Operation.LOCAL_DELETE, oldrec
							.getName(), stub.storage().localToRemote()
							.get(oldrec.getName())));
					changes.add(new ChangeRecord(Operation.LOCAL_INSERT, newrec
							.getName(), null));
				} else if (!oldrec.getMd5Checksum().equals(
						newrec.getMd5Checksum())) {
					changes.addAll(compare(oldrec, newrec));
				}
			}
			// Check files with same md5 but different names
			List<String> newNames = new ArrayList<String>();
			newNames.addAll(ccNameTable.keySet());

			for (String newName : newNames) {
				String md5 = ccNameTable.get(newName).getMd5Checksum();
				if (scMd5Table.containsKey(md5)) {
					Snapshot oldrec = scMd5Table.get(md5);
					Snapshot newrec = ccMd5Table.get(md5);
					changes.add(new ChangeRecord(Operation.LOCAL_RENAME, oldrec
							.getName(), stub.storage().localToRemote()
							.get(oldrec.getName()), DriveUtils.absolutePath(
							newrec.getName()).getName()));
					scNameTable.remove(oldrec.getName());
					ccNameTable.remove(newName);
				}
			}
			// Deleted files
			for (Entry<String, Snapshot> old : scNameTable.entrySet()) {
				changes.add(new ChangeRecord(Operation.LOCAL_DELETE, old
						.getValue().getName(), stub.storage().localToRemote()
						.get(old.getValue().getName())));
			}

			// New files
			for (Entry<String, Snapshot> newe : ccNameTable.entrySet()) {
				changes.add(new ChangeRecord(Operation.LOCAL_INSERT, newe
						.getValue().getName(), null));
			}
		}
		return changes;
	}

	protected long changes(List<Change> changes) throws IOException {
		com.google.api.services.drive.Drive.Changes.List request = drive
				.changes().list();
		request.setMaxResults(100);
		long lastChange = -1;
		Object lastChangeData = stub.storage()
				.get(StorageService.REMOTE_CHANGE);
		if (lastChangeData != null) {
			if (lastChangeData instanceof Double) {
				lastChange = ((Double) lastChangeData).longValue();
				request.setStartChangeId(lastChange + 1);
			} else if (lastChangeData instanceof Long) {
				lastChange = (Long) lastChangeData;
				request.setStartChangeId(lastChange + 1);
			} else {
				throw new IllegalArgumentException(lastChangeData.getClass()
						.getName());
			}
		}

		while (true) {
			ChangeList response = execute(request);
			if (changes != null) {
				changes.addAll(response.getItems());
			}
			if (StringUtils.isEmpty(response.getNextPageToken())) {
				List<Change> items = response.getItems();
				if (!items.isEmpty())
					lastChange = items.get(items.size() - 1).getId();
				break;
			} else {
				request.setPageToken(response.getNextPageToken());
			}
		}
		return lastChange;
	}
}
