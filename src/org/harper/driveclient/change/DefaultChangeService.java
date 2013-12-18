package org.harper.driveclient.change;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.apache.commons.collections4.CollectionUtils;
import org.harper.driveclient.Constants;
import org.harper.driveclient.common.DefaultService;
import org.harper.driveclient.snapshot.Snapshot;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.ChildReference;

public class DefaultChangeService extends DefaultService implements
		ChangeService {

	public DefaultChangeService(Drive d) {
		super(d);
	}

	@Override
	public List<ChangeRecord> compare(File localRoot, String remoteRoot,
			Snapshot snapshot) throws IOException {
		List<ChangeRecord> records = new ArrayList<ChangeRecord>();

		// In the case that snapshot is null, make a union. Files online have a
		// higher priority.
		if (snapshot == null) {
			File[] localFolders = localRoot.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return pathname.isDirectory();
				}
			});
			File[] localFiles = localRoot.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return pathname.isFile();
				}
			});
			Map<String, File> localFolderTable = new HashMap<String, File>();
			Map<String, File> localFileTable = new HashMap<String, File>();
			if (localFolders != null)
				for (File f : localFolders) {
					localFolderTable.put(f.getName(), f);
				}
			if (localFiles != null)
				for (File f : localFiles) {
					localFileTable.put(f.getName(), f);
				}

			Map<String, com.google.api.services.drive.model.File> remoteFolderTable = new HashMap<String, com.google.api.services.drive.model.File>();
			Map<String, com.google.api.services.drive.model.File> remoteFileTable = new HashMap<String, com.google.api.services.drive.model.File>();
			List<com.google.api.services.drive.model.File> remoteFiles = list(remoteRoot);
			for (com.google.api.services.drive.model.File f : remoteFiles) {
				if (Constants.TYPE_FOLDER.equals(f.getMimeType())) {
					remoteFolderTable.put(f.getTitle(), f);
				} else {
					remoteFileTable.put(f.getTitle(), f);
				}
			}

			Collection<String> localToRemoteFolders = CollectionUtils.subtract(
					localFolderTable.keySet(), remoteFolderTable.keySet());
			Collection<String> remoteToLocalFolders = CollectionUtils.subtract(
					remoteFolderTable.keySet(), localFolderTable.keySet());
			Collection<String> commonFolders = CollectionUtils.intersection(
					remoteFolderTable.keySet(), localFolderTable.keySet());

			for (String key : localToRemoteFolders) {
				records.add(new ChangeRecord(ChangeRecord.UPLOAD,
						localFolderTable.get(key), remoteRoot));
			}
			for (String key : remoteToLocalFolders) {
				records.add(new ChangeRecord(ChangeRecord.DOWNLOAD, localRoot,
						remoteFolderTable.get(key).getId()));
			}
			for (String key : commonFolders) {
				records.addAll(compare(localFolderTable.get(key),
						remoteFolderTable.get(key).getId(), null));
			}

			Collection<String> localToRemoteFiles = CollectionUtils.subtract(
					localFileTable.keySet(), remoteFileTable.keySet());
			Collection<String> remoteToLocalFiles = CollectionUtils.subtract(
					remoteFileTable.keySet(), localFileTable.keySet());
			Collection<String> commonFiles = CollectionUtils.intersection(
					localFileTable.keySet(), remoteFileTable.keySet());
			for (String key : localToRemoteFiles) {
				records.add(new ChangeRecord(ChangeRecord.UPLOAD,
						localFileTable.get(key), remoteRoot));
			}
			for (String key : remoteToLocalFiles) {
				records.add(new ChangeRecord(ChangeRecord.DOWNLOAD, localRoot,
						remoteFileTable.get(key).getId()));
			}
			for (String key : commonFiles) {
				// Compare MD5
				String remoteMd5 = remoteFileTable.get(key).getMd5Checksum();
				String localMd5 = md5Checksum(localFileTable.get(key));
				if (!remoteMd5.equals(localMd5)) {
					records.add(new ChangeRecord(ChangeRecord.DOWNLOAD,
							localRoot, remoteFileTable.get(key).getId()));
				}
			}
		} else {

		}
		return records;
	}

	protected List<com.google.api.services.drive.model.File> list(String root)
			throws IOException {
		List<com.google.api.services.drive.model.File> result = new ArrayList<com.google.api.services.drive.model.File>();
		List<ChildReference> children = drive.children().list(root).execute()
				.getItems();
		for (ChildReference c : children) {
			result.add(drive.files().get(c.getId()).execute());
		}
		return result;
	}

	public Map<String, String> remoteMd5(String root) throws IOException {
		Map<String, String> result = new HashMap<String, String>();
		com.google.api.services.drive.model.File rootFile = drive.files().get(root).execute();
		List<ChildReference> children = drive.children().list(root).execute()
				.getItems();
		PriorityQueue<String> sorting = new PriorityQueue<String>();
		for (ChildReference child : children) {
			com.google.api.services.drive.model.File file = drive.files()
					.get(child.getId()).execute();
			if (Constants.TYPE_FOLDER.equals(file.getMimeType())) {
				result.putAll(remoteMd5(file.getId()));
				sorting.offer(result.get(file.getId())+file.getTitle());
			} else if (file.getMimeType().startsWith(
					Constants.TYPE_GDOCS_PREFIX)) {
				String md5 = md5Checksum(file.getDefaultOpenWithLink());
				result.put(file.getId(), md5);
				sorting.offer(md5+file.getTitle());
			} else {
				String md5 = file.getMd5Checksum();
				result.put(file.getId(), md5);
				sorting.offer(md5+file.getTitle());
			}
		}
		StringBuilder sb = new StringBuilder();
		sb.append(rootFile.getTitle());
		while (!sorting.isEmpty()) {
			sb.append(sorting.poll());
		}
		result.put(root, md5Checksum(sb.toString()));
		return result;
	}

	protected String md5Checksum(File localFile) throws IOException {
		try {
			MessageDigest md5 = MessageDigest.getInstance("md5");
			byte[] buffer = new byte[10000];
			FileInputStream fis = new FileInputStream(localFile);
			int count = 0;
			while ((count = fis.read(buffer)) != 0) {
				md5.update(buffer, 0, count);
			}
			fis.close();
			return new String(md5.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		} catch (IOException e) {
			throw e;
		}
	}

	protected String md5Checksum(String content) {
		try {
			MessageDigest md5 = MessageDigest.getInstance("md5");
			md5.update(content.getBytes());
			return new String(md5.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
