package org.harper.driveclient.synchronize;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import org.apache.commons.collections4.CollectionUtils;
import org.harper.driveclient.Configuration;
import org.harper.driveclient.Constants;
import org.harper.driveclient.Services;
import org.harper.driveclient.common.DefaultService;
import org.harper.driveclient.common.DriveUtils;
import org.harper.driveclient.common.StringUtils;
import org.harper.driveclient.snapshot.Snapshot;
import org.harper.driveclient.storage.StorageService;
import org.harper.driveclient.synchronize.operation.LocalChange;
import org.harper.driveclient.synchronize.operation.LocalDelete;
import org.harper.driveclient.synchronize.operation.LocalInsert;
import org.harper.driveclient.synchronize.operation.LocalRename;
import org.harper.driveclient.synchronize.operation.RemoteChange;
import org.harper.driveclient.synchronize.operation.RemoteDelete;
import org.harper.driveclient.synchronize.operation.RemoteInsert;
import org.harper.driveclient.synchronize.operation.RemoteMove;
import org.harper.driveclient.synchronize.operation.RemoteRename;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.ChangeList;
import com.google.api.services.drive.model.ChildReference;

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
		if (!localRoot.exists()) {
			localRoot.mkdirs();
		}
		String localRelative = DriveUtils.relativePath(localRoot);
		stub.storage().remoteToLocal().put(remoteRoot, localRelative);
		stub.storage().localToRemote().put(localRelative, remoteRoot);

		for (ChildReference childref : drive.children().list(remoteRoot)
				.execute().getItems()) {
			stub.transmit().download(childref.getId(), localRoot);
		}

		stub.storage().put(StorageService.SNAPSHOT, stub.snapshot().make());
	}

	@Override
	public void synchronize() throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("Synchronizing...");
		}
		// Detect remote change
		List<Operation> remoteChanges = remoteChange();
		// Detect local change
		Snapshot standard = stub.storage().get(StorageService.SNAPSHOT);
		Snapshot current = stub.snapshot().make();
		List<Operation> localChanges = compare(standard, current);

		/*
		 * First upload then download. Local change has higher priority than
		 * remote because remote change can be restored while local cannot.
		 * Recorded uploaded file so later no need to download
		 */

		Map<String, String> localUploaded = new HashMap<String, String>();
		for (Operation localChange : localChanges) {
			synchronize(localChange);
			localUploaded.put(localChange.getLocalFile(),
					localChange.getRemoteFileId());
		}

		PriorityQueue<Operation> orderedRemoteChanges = new PriorityQueue<Operation>(
				50, new Comparator<Operation>() {
					@Override
					public int compare(Operation o1, Operation o2) {
						if (o1.getName().compareTo(o2.getName()) != 0) {
							return o1.getName().compareTo(o2.getName());
						} else {
							if (o1.getLocalFile() == null)
								return -1;
							if (o2.getLocalFile() == null)
								return 1;
							return o1.getLocalFile().compareTo(
									o2.getLocalFile());
						}
					}
				});

		for (Operation remoteChange : remoteChanges) {
			if (!localUploaded.containsKey(remoteChange.getLocalFile())) {
				synchronize(remoteChange);
				// As we process local change first
				// the remote insert may be caused by local upload
				if (remoteChange instanceof RemoteInsert) {
					((RemoteInsert) remoteChange).setLocalFile(stub.storage()
							.remoteToLocal()
							.get(remoteChange.getRemoteFileId()));
				}
				orderedRemoteChanges.add(remoteChange);
			}
		}

		while (!orderedRemoteChanges.isEmpty()) {
			orderedRemoteChanges.poll().updateSnapshot(current);
		}

		// Retry until all the errors are processed
		while (!stub.storage().failedLog().isEmpty()) {
			FailedRecord fr = stub.storage().failedLog().remove(0);
			// Filter, modify and discard
			fr = correct(fr);
			if (fr != null) {
				synchronize(fr.getOperation());
			}
		}
		current.update();
		stub.storage().put(StorageService.SNAPSHOT, current);
	}

	private FailedRecord correct(FailedRecord fr) {
		if (fr.getOperation() instanceof LocalChange
				|| fr.getOperation() instanceof LocalRename) {
			if (StringUtils.isEmpty(fr.getOperation().getRemoteFileId())
					|| StringUtils.isEmpty(fr.getError())) {
				fr.setOperation(new LocalInsert(fr.getOperation()));
			} else if (!StringUtils.isEmpty(fr.getError())
					&& fr.getError().startsWith("Local file doesn't exist")) {
				return null;
			}
		}
		if (fr.getOperation() instanceof LocalInsert) {
			if (!StringUtils.isEmpty(fr.getError())
					&& fr.getError().startsWith("Local file doesn't exist")) {
				return null;
			}
		}
		if (fr.getOperation() instanceof LocalDelete
				&& (StringUtils.isEmpty(fr.getOperation().getRemoteFileId()) || "404"
						.equals(fr.getError()))) {
			return null;
		}
		if ("404".equals(fr.getError())
				&& (fr.getOperation() instanceof LocalChange
						|| fr.getOperation() instanceof LocalDelete || fr
							.getOperation() instanceof LocalRename)) {
			// No such file
			fr.setOperation(new LocalInsert(fr.getOperation()));
			return fr;
		}
		if ("404".equals(fr.getError())
				&& (fr.getOperation() instanceof RemoteInsert
						|| fr.getOperation() instanceof RemoteChange
						|| fr.getOperation() instanceof RemoteRename || fr
							.getOperation() instanceof RemoteDelete)) {
			return null;
		}
		return fr;
	}

	private void synchronize(Operation operation) throws IOException {
		try {
			operation.execute();
		} catch (Exception e) {
			logger.warn(MessageFormat.format(
					"Exception on Change {0}. Retry later", operation), e);
			FailedRecord fr = new FailedRecord(operation);
			if (e instanceof GoogleJsonResponseException) {
				GoogleJsonResponseException gjre = (GoogleJsonResponseException) e;
				fr.setError(String.valueOf(gjre.getDetails().getCode()));
			} else {
				fr.setError(e.getMessage());
			}
			stub.storage().failedLog().add(fr);
		}
	}

	private List<Operation> remoteChange() throws IOException {
		List<Change> changes = new ArrayList<Change>();
		long lastChange = changes(changes);
		stub.storage().put(StorageService.REMOTE_CHANGE, lastChange);
		List<Operation> records = new ArrayList<Operation>();

		for (Change remoteChange : changes) {
			String local = stub.storage().remoteToLocal()
					.get(remoteChange.getFileId());
			if (remoteChange.getDeleted()) {
				records.add(new RemoteDelete(local, remoteChange.getFileId(),
						remoteChange.getFile()));
			} else if (remoteChange.getFile().getLabels().getTrashed()) {
				records.add(new RemoteDelete(local, remoteChange.getFileId(),
						remoteChange.getFile()));
			} else if (StringUtils.isEmpty(local)) {
				// Cannot find this file, should be new
				records.add(new RemoteInsert(null, remoteChange.getFileId(),
						remoteChange.getFile()));
			} else {
				if (!DriveUtils.isDirectory(remoteChange.getFile())
						&& !DriveUtils.isGoogleDoc(remoteChange.getFile())) {
					String remoteName = remoteChange.getFile().getTitle();
					String remoteMd5 = remoteChange.getFile().getMd5Checksum();
					File localFile = DriveUtils.absolutePath(local);
					if (localFile.isFile()) {
						String localMd5 = DriveUtils.md5Checksum(localFile);
						if (localMd5.equals(remoteMd5)) {
							// Sometimes google drive will record change even if
							// the name and MD5 are exactly the same
							if (!remoteName.equals(localFile.getName())) {
								records.add(new RemoteRename(local,
										remoteChange.getFileId(), remoteChange
												.getFile()));
							} else {
								// Handle file movement, aka change of parent
								String localParent = localFile.getParent();
								String originRemoteId = getStub().storage()
										.localToRemote().get(localParent);
								String remoteId = remoteChange.getFile()
										.getParents().get(0).getId();
								if (originRemoteId != null
										&& !originRemoteId.equals(remoteId)) {
									// Moved
									records.add(new RemoteMove(local,
											remoteChange.getFileId(),
											remoteChange.getFile()));
								}
							}
						} else {
							records.add(new RemoteChange(local, remoteChange
									.getFileId(), remoteChange.getFile()));
						}
					} else {
						records.add(new RemoteInsert(null, remoteChange
								.getFileId(), remoteChange.getFile()));
					}

				} else {
					// For directory check the name change and parent change
					String remoteName = remoteChange.getFile().getTitle();
					String localName = DriveUtils.absolutePath(local).getName();
					if (!remoteName.equals(localName)) {
						records.add(new RemoteRename(local, remoteChange
								.getFileId(), remoteChange.getFile()));
					}

					String newParentRemoteId = remoteChange.getFile()
							.getParents().get(0).getId();
					String currentParent = DriveUtils.absolutePath(local)
							.getParent();
					String currentParentRemoteId = stub.storage()
							.localToRemote().get(currentParent);
					if (!newParentRemoteId.equals(currentParentRemoteId)) {
						// Parent changed
						if (stub.storage().remoteToLocal()
								.containsKey(newParentRemoteId)) {
							// Moved to existing parent
							records.add(new RemoteMove(local, remoteChange
									.getFileId(), remoteChange.getFile()));
						} else {
							// The parent itself is new, insertion for the
							// parent should be somewhere, just delete the old
							// file
							records.add(new RemoteDelete(local, remoteChange
									.getFileId(), remoteChange.getFile()));
						}
					}
				}
			}
		}

		return records;
	}

	protected List<Operation> compare(Snapshot standard, Snapshot current) {
		List<Operation> changes = new ArrayList<Operation>();

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
			changes.add(new LocalChange(standard.getName(), stub.storage()
					.localToRemote().get(standard.getName())));
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
					changes.add(new LocalDelete(oldrec.getName(), stub
							.storage().localToRemote().get(oldrec.getName())));
					changes.add(new LocalInsert(newrec.getName(), null));
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
						changes.add(new LocalRename(oldrec.getName(), stub
								.storage().localToRemote()
								.get(oldrec.getName()), DriveUtils
								.absolutePath(newrec.getName()).getName()));
					}
					while (!newrecs.isEmpty()) {
						// Copy
						Snapshot newrec = newrecs.remove(0);
						changes.add(new LocalInsert(newrec.getName(), null));
					}
					scNameTable.remove(firstOld.getName());
					ccNameTable.remove(newName);
				}
				// New files will be processed below
			}
			// Deleted files
			for (Entry<String, Snapshot> old : scNameTable.entrySet()) {
				changes.add(new LocalDelete(old.getValue().getName(), stub
						.storage().localToRemote()
						.get(old.getValue().getName())));
			}

			// New files
			for (Entry<String, Snapshot> newe : ccNameTable.entrySet()) {
				changes.add(new LocalInsert(newe.getValue().getName(), null));
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

	/*
	 * Depth first search for a parent that corresponds a local file
	 */

}
