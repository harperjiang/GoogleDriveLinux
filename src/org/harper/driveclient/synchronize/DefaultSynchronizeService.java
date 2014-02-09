package org.harper.driveclient.synchronize;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
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

		// Retry until all the errors are processed
		while (!stub.storage().failedLog().isEmpty()) {
			FailedRecord fr = stub.storage().failedLog().remove(0);
			// Filter, modify and discard
			fr = correct(fr);
			if (fr != null) {
				synchronize(fr);
			}
		}

		// Save the new snapshot
		stub.storage().put(StorageService.SNAPSHOT, current);
	}

	private FailedRecord correct(FailedRecord fr) {
		if (fr.getOperation() == Operation.LOCAL_CHANGE
				|| fr.getOperation() == Operation.LOCAL_RENAME) {
			if (StringUtils.isEmpty(fr.getRemoteFileId())) {
				fr.setOperation(Operation.LOCAL_INSERT);
			}
			if (fr.getError().startsWith("Local file doesn't exist")) {
				return null;
			}
		}
		if (fr.getOperation() == Operation.LOCAL_INSERT) {
			if (fr.getError().startsWith("Local file doesn't exist")) {
				return null;
			}
		}
		if (fr.getOperation() == Operation.LOCAL_DELETE
				&& StringUtils.isEmpty(fr.getRemoteFileId())) {
			return null;
		}
		if ((fr.getOperation() == Operation.REMOTE_INSERT
				|| fr.getOperation() == Operation.REMOTE_CHANGE
				|| fr.getOperation() == Operation.REMOTE_RENAME || fr
				.getOperation() == Operation.REMOTE_DELETE)
				&& "404".equals(fr.getError())) {
			return null;
		}
		return fr;
	}

	private void synchronize(ChangeRecord record) throws IOException {
		try {
			switch (record.getOperation()) {
			case LOCAL_INSERT: {
				// Get remote id for the parent folder
				File local = DriveUtils.absolutePath(record.getLocalFile());
				String remoteParent = stub.storage().localToRemote()
						.get(DriveUtils.relativePath(local.getParentFile()));
				if (!StringUtils.isEmpty(remoteParent)
						&& !stub.storage().localToRemote()
								.containsKey(record.getLocalFile())) {
					// Ignore insert request that doesn't have a parent or
					// already have a mapping.
					// The insert operation will be accomplished by the topmost
					// folder
					stub.transmit().upload(remoteParent, local);
				}
				break;
			}
			case LOCAL_DELETE: {
				if (!StringUtils.isEmpty(record.getRemoteFileId())) {
					stub.transmit().delete(record.getRemoteFileId());
				} else {
					logger.warn("Local deletion has no remote reference, make sure the storage is correct.");
				}
				break;
			}
			case LOCAL_CHANGE: {
				if (!StringUtils.isEmpty(record.getRemoteFileId())) {
					File local = DriveUtils.absolutePath(record.getLocalFile());
					stub.transmit().update(record.getRemoteFileId(), local);
				} else {
					logger.warn("Local change has no remote reference, make sure the storage is correct.");
				}
				break;
			}
			case LOCAL_RENAME: {
				if (!StringUtils.isEmpty(record.getRemoteFileId())) {
					stub.transmit().rename(record.getRemoteFileId(),
							(String) record.getContext()[0]);
				} else {
					logger.warn("Local rename has no remote reference, make sure the storage is correct.");
				}
				break;
			}
			case REMOTE_INSERT: {
				if (stub.storage().remoteToLocal()
						.containsKey(record.getRemoteFileId())) {
					// This file/folder already had been created
					break;
				}
				// Depth search of parent that has a local root
				List<ParentReference> path = new ArrayList<ParentReference>();
				File local = pathToLocal(record.getRemoteFileId(), path);
				File parent = local;
				for (ParentReference node : path) {
					stub.transmit().download(node.getId(), parent);
					parent = DriveUtils.absolutePath(stub.storage()
							.remoteToLocal().get(node.getId()));
				}
				stub.transmit().download(record.getRemoteFileId(), parent);
				break;
			}
			case REMOTE_DELETE: {
				if (StringUtils.isEmpty(record.getLocalFile())) {
					// No local file, remote file should be a trashed one.
					if (logger.isDebugEnabled()) {
						logger.debug("No corresponding local file for deletion. Remote file may be trashed");
					}
					break;
				}
				File localFile = DriveUtils.absolutePath(record.getLocalFile());
				deleteLocalFile(localFile);
				if (localFile.exists()) {
					if (logger.isDebugEnabled()) {
						logger.debug(MessageFormat
								.format("Failed to delete file {0}. File may have been deleted.",
										record.getLocalFile()));
					}
				}
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
					stub.storage().localToRemote()
							.remove(record.getLocalFile());
					stub.storage().remoteToLocal()
							.remove(record.getRemoteFileId());
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
		} catch (Exception e) {
			logger.warn(MessageFormat.format(
					"Exception on Change {0}. Retry later", record), e);
			FailedRecord fr = new FailedRecord(record);
			fr.setError(e.getMessage());
			stub.storage().failedLog().add(fr);
		}
	}

	private List<ChangeRecord> remoteChange() throws IOException {
		List<Change> changes = new ArrayList<Change>();
		long lastChange = changes(changes);
		stub.storage().put(StorageService.REMOTE_CHANGE, lastChange);
		List<ChangeRecord> records = new ArrayList<ChangeRecord>();

		for (Change remoteChange : changes) {
			String local = stub.storage().remoteToLocal()
					.get(remoteChange.getFileId());
			if (remoteChange.getDeleted()) {
				records.add(new ChangeRecord(Operation.REMOTE_DELETE, local,
						remoteChange.getFileId()));
			} else if (remoteChange.getFile().getLabels().getTrashed()) {
				records.add(new ChangeRecord(Operation.REMOTE_DELETE, local,
						remoteChange.getFileId()));
			} else if (StringUtils.isEmpty(local)) {
				// Cannot find this file, should be new
				records.add(new ChangeRecord(Operation.REMOTE_INSERT, null,
						remoteChange.getFileId()));
			} else {
				if (!DriveUtils.isDirectory(remoteChange.getFile())) {
					String remoteName = remoteChange.getFile().getTitle();
					String remoteMd5 = remoteChange.getFile().getMd5Checksum();
					File localFile = DriveUtils.absolutePath(local);
					if (localFile.isFile()) {
						String localMd5 = DriveUtils.md5Checksum(localFile);
						if (localMd5.equals(remoteMd5)) {
							// Sometimes google drive will record change even if
							// the name and MD5 are exactly the same
							if (!remoteName.equals(localFile.getName())) {
								records.add(new ChangeRecord(
										Operation.REMOTE_RENAME, local,
										remoteChange.getFileId(), remoteName));
							}
						} else {
							records.add(new ChangeRecord(
									Operation.REMOTE_CHANGE, local,
									remoteChange.getFileId()));
						}
					} else {
						records.add(new ChangeRecord(Operation.REMOTE_INSERT,
								null, remoteChange.getFileId()));
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
			Map<String, List<Snapshot>> scMd5Table = new HashMap<String, List<Snapshot>>();
			Map<String, Snapshot> scNameTable = new HashMap<String, Snapshot>();
			Map<String, List<Snapshot>> ccMd5Table = new HashMap<String, List<Snapshot>>();
			Map<String, Snapshot> ccNameTable = new HashMap<String, Snapshot>();
			for (Snapshot sc : standard.getChildren()) {
				if (!scMd5Table.containsKey(sc.getMd5Checksum())) {
					scMd5Table.put(sc.getMd5Checksum(),
							new ArrayList<Snapshot>());
				}
				scMd5Table.get(sc.getMd5Checksum()).add(sc);
				scNameTable.put(sc.getName(), sc);
			}
			for (Snapshot cc : current.getChildren()) {
				if (!ccMd5Table.containsKey(cc.getMd5Checksum())) {
					ccMd5Table.put(cc.getMd5Checksum(),
							new ArrayList<Snapshot>());
				}
				ccMd5Table.get(cc.getMd5Checksum()).add(cc);
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
			// Also deal with copied files
			List<String> newNames = new ArrayList<String>();
			newNames.addAll(ccNameTable.keySet());

			for (String newName : newNames) {
				String md5 = ccNameTable.get(newName).getMd5Checksum();
				if (scMd5Table.containsKey(md5)) {
					List<Snapshot> oldrecs = scMd5Table.get(md5);
					List<Snapshot> newrecs = ccMd5Table.get(md5);
					Snapshot firstOld = oldrecs.get(0);
					while (!oldrecs.isEmpty()) {
						Snapshot oldrec = oldrecs.remove(0);
						Snapshot newrec = newrecs.remove(0);
						// Rename
						changes.add(new ChangeRecord(Operation.LOCAL_RENAME,
								oldrec.getName(), stub.storage()
										.localToRemote().get(oldrec.getName()),
								DriveUtils.absolutePath(newrec.getName())
										.getName()));
					}
					while (!newrecs.isEmpty()) {
						// Copy
						Snapshot newrec = newrecs.remove(0);
						changes.add(new ChangeRecord(Operation.LOCAL_INSERT,
								newrec.getName(), null));
					}
					scNameTable.remove(firstOld.getName());
					ccNameTable.remove(newName);
				}
				// New files will be processed below
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

	private File pathToLocal(String remoteFileId, List<ParentReference> path)
			throws IOException {
		List<ParentReference> pRefs = drive.parents().list(remoteFileId)
				.execute().getItems();
		for (ParentReference pRef : pRefs) {
			if (pRef.getIsRoot()) {
				return Configuration.getLocalRoot();
			}
			if (stub.storage().remoteToLocal().containsKey(pRef.getId())) {
				return DriveUtils.absolutePath(stub.storage().remoteToLocal()
						.get(pRef.getId()));
			} else {
				path.add(pRef);
				File lookInside = pathToLocal(pRef.getId(), path);
				if (null != lookInside) {
					return lookInside;
				} else {
					path.remove(pRef);
				}
			}
		}
		return null;
	}

	private void deleteLocalFile(File file) {
		if (!file.exists())
			return;
		// Execute a recursive delete
		if (file.isFile()) {
			file.delete();
		} else {
			for (File child : file.listFiles()) {
				deleteLocalFile(child);
			}
			file.delete();
		}
	}
}
