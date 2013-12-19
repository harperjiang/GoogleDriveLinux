package org.harper.driveclient.change;

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
import com.google.api.services.drive.model.File;

public class DefaultChangeService extends DefaultService implements
		ChangeService {

	public DefaultChangeService(Drive d) {
		super(d);
	}

	@Override
	public List<ChangeRecord> compare(java.io.File localRoot,
			String remoteRoot, Snapshot snapshot) throws IOException {
		List<ChangeRecord> records = new ArrayList<ChangeRecord>();

		// In the case that snapshot is null, make a union. Files online have a
		// higher priority.
		if (snapshot == null) {
			java.io.File[] localFolders = localRoot.listFiles(new FileFilter() {
				@Override
				public boolean accept(java.io.File pathname) {
					return pathname.isDirectory();
				}
			});
			java.io.File[] localFiles = localRoot.listFiles(new FileFilter() {
				@Override
				public boolean accept(java.io.File pathname) {
					return pathname.isFile();
				}
			});
			Map<String, java.io.File> localFolderTable = new HashMap<String, java.io.File>();
			Map<String, java.io.File> localFileTable = new HashMap<String, java.io.File>();
			if (localFolders != null)
				for (java.io.File f : localFolders) {
					localFolderTable.put(f.getName(), f);
				}
			if (localFiles != null)
				for (java.io.File f : localFiles) {
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

	protected List<File> list(String root) throws IOException {
		List<File> result = new ArrayList<File>();
		List<ChildReference> children = drive.children().list(root).execute()
				.getItems();
		for (ChildReference c : children) {
			result.add(drive.files().get(c.getId()).execute());
		}
		return result;
	}

	public Map<String, String> remoteMd5() throws IOException {
		Map<String, File> fileContext = new HashMap<String, File>();
		com.google.api.services.drive.Drive.Files.List request = drive.files()
				.list();
		request.setQ("trashed = false");
		
		request.setPageToken(arg0)
		for (File file : request.execute().getItems()) {
			fileContext.put(file.getId(), file);
		}
		Map<String, String> result = new HashMap<String, String>();
		fileMd5(Constants.FOLDER_ROOT, fileContext, result);
		return result;
	}

	protected void fileMd5(String current, Map<String, File> fileContext,
			Map<String, String> md5Context) throws IOException {
		File currentFile = fileContext.get(current);
		if (null == currentFile && !Constants.FOLDER_ROOT.equals(current)) {
			currentFile = drive.files().get(current).execute();
		}
		List<ChildReference> children = drive.children().list(current)
				.execute().getItems();
		PriorityQueue<String> sorting = new PriorityQueue<String>();
		for (ChildReference child : children) {
			File childFile = fileContext.get(child.getId());
			if (null == childFile) {
				childFile = drive.files().get(child.getId()).execute();
			}
			if (Constants.TYPE_FOLDER.equals(childFile.getMimeType())) {
				fileMd5(childFile.getId(), fileContext, md5Context);
				sorting.offer(md5Context.get(childFile.getId())
						+ childFile.getTitle());
			} else if (childFile.getMimeType().startsWith(
					Constants.TYPE_GDOCS_PREFIX)) {
				String md5 = md5Checksum(childFile.getDefaultOpenWithLink());
				md5Context.put(childFile.getId(), md5);
				sorting.offer(md5 + childFile.getTitle());
			} else {
				String md5 = childFile.getMd5Checksum();
				md5Context.put(childFile.getId(), md5);
				sorting.offer(md5 + childFile.getTitle());
			}
		}
		StringBuilder sb = new StringBuilder();
		if (!Constants.FOLDER_ROOT.equals(current)) {
			sb.append(currentFile.getTitle());
		}
		while (!sorting.isEmpty()) {
			sb.append(sorting.poll());
		}
		md5Context.put(current, md5Checksum(sb.toString()));
	}

	protected String md5Checksum(java.io.File localFile) throws IOException {
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
