package org.harper.driveclient.snapshot;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.harper.driveclient.Configuration;
import org.harper.driveclient.Constants;
import org.harper.driveclient.Services;
import org.harper.driveclient.common.DefaultService;
import org.harper.driveclient.common.DriveUtils;
import org.harper.driveclient.common.StringUtils;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

public class DefaultSnapshotService extends DefaultService implements
		SnapshotService {

	public DefaultSnapshotService(Drive drive, Services stub) {
		super(drive, stub);
	}

	@Override
	public Snapshot get() {
		return null;
	}

	@Override
	public Snapshot make() throws IOException {
		java.io.File root = Configuration.getLocalRoot();
		return make(root);
	}

	protected Snapshot make(java.io.File root) {
		if (!root.exists())
			return null;
		Snapshot current = new Snapshot();
		current.setName(DriveUtils.relativePath(root));
		if (root.isDirectory()) {
			current.setFile(false);
			java.io.File[] children = root.listFiles();
			if (children != null) {
				for (java.io.File child : children) {
					current.addChild(make(child));
				}
			}
			// Sort children in alphabet order, and calculate the md5 of parent
			Map<String, String> md5s = new HashMap<String, String>();
			PriorityQueue<String> sort = new PriorityQueue<String>();
			for (Snapshot sc : current.getChildren()) {
				md5s.put(sc.getName(), sc.getMd5Checksum());
				sort.offer(sc.getName());
			}
			StringBuilder sb = new StringBuilder();
			while (!sort.isEmpty()) {
				sb.append(md5s.get(sort.poll()));
			}
			current.setMd5Checksum(DriveUtils.md5Checksum(sb.toString()));
		} else {
			current.setFile(true);
			current.setMd5Checksum(DriveUtils.md5Checksum(root));
		}
		return current;
	}

	public Map<String, String> remoteMd5() throws IOException {
		Map<String, File> fileContext = new HashMap<String, File>();
		com.google.api.services.drive.Drive.Files.List request = drive.files()
				.list();
		request.setQ("trashed = false");
		FileList response = null;
		while (true) {
			response = request.execute();
			for (File file : response.getItems()) {
				fileContext.put(file.getId(), file);
			}
			if (StringUtils.isEmpty(response.getNextPageToken()))
				break;
			request.setPageToken(response.getNextPageToken());
		}
		Map<String, String> result = new HashMap<String, String>();
		fileMd5(Constants.FOLDER_ROOT, fileContext, result);
		return result;
	}

	protected void fileMd5(String current, Map<String, File> fileContext,
			Map<String, String> md5Context) throws IOException {
		File currentFile = fileContext.get(current);
		List<ChildReference> children = drive.children().list(current)
				.execute().getItems();
		PriorityQueue<String> sorting = new PriorityQueue<String>();
		for (ChildReference child : children) {
			File childFile = fileContext.get(child.getId());
			if (Constants.TYPE_FOLDER.equals(childFile.getMimeType())) {
				fileMd5(childFile.getId(), fileContext, md5Context);
				sorting.offer(md5Context.get(childFile.getId())
						+ childFile.getTitle());
			} else if (childFile.getMimeType().startsWith(
					Constants.TYPE_GDOCS_PREFIX)) {
				String md5 = DriveUtils.md5Checksum(childFile
						.getDefaultOpenWithLink());
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
		md5Context.put(current, DriveUtils.md5Checksum(sb.toString()));
	}

	@Override
	public String localMd5(java.io.File root, Map<String, String> context)
			throws IOException {
		String md5 = null;
		if (root.isDirectory()) {
			java.io.File[] children = root.listFiles();
			Map<String, java.io.File> table = new HashMap<String, java.io.File>();
			PriorityQueue<String> queue = new PriorityQueue<String>();
			for (java.io.File child : children) {
				table.put(child.getName(), child);
				queue.offer(child.getName());
			}
			StringBuilder sb = new StringBuilder();
			sb.append(root.getName());
			while (!queue.isEmpty()) {
				sb.append(localMd5(table.get(queue.poll()), context));
			}
			md5 = DriveUtils.md5Checksum(sb.toString());
		} else {
			md5 = DriveUtils.md5Checksum(root);
		}
		context.put(root.getAbsolutePath(), md5);
		return md5;
	}

}
