package org.harper.driveclient.shell;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.harper.driveclient.Constants;
import org.harper.driveclient.Shell;
import org.harper.driveclient.common.StringUtils;

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;

public class Upload extends Command {

	private String[] inputs;

	private Shell shell;

	public Upload(String input) {
		this.inputs = input.split("\\s+");
	}

	@Override
	public void execute(Shell shell) throws Exception {
		if (inputs.length < 2)
			return;
		this.shell = shell;
		String remoteRoot = shell.getCurrentFolder();
		java.io.File localRoot = new java.io.File(inputs[1]);
		if (localRoot.exists()) {
			System.err.println("Cannot find file to upload");
			return;
		}

		// Generate and save the list to upload in a file
		System.out.println("Counting files to upload...");
		java.util.List<java.io.File> files = new ArrayList<>();
		java.util.List<java.io.File> folders = new ArrayList<>();
		iterate(localRoot, files, folders);

		// Save all the files to transfer in file
		PrintWriter pw = new PrintWriter(new FileOutputStream("upload.all"));
		AtomicLong size = new AtomicLong(0);
		files.forEach((java.io.File f) -> {
			if (f.isFile()) {
				pw.println(f.getAbsolutePath());
				size.addAndGet(f.length());
			}
		});
		pw.close();

		System.out.println(MessageFormat.format("Total {0} files to upload, size {1}", files.size(), size.get()));

		// Create all folders
		Map<String, String> folderMapping = new HashMap<>();
		String remoteFolder = mkdir(remoteRoot, localRoot);
		folderMapping.put(localRoot.getAbsolutePath(), remoteFolder);

		folders.forEach((java.io.File folder) -> {
			if (!folderMapping.containsKey(folder.getAbsolutePath())) {
				String parentId = folderMapping.get(folder.getParentFile().getAbsolutePath());
				if (StringUtils.isEmpty(parentId)) {
					throw new IllegalStateException("No parent for " + folder.getAbsolutePath());
				}
				String remoteId = mkdir(parentId, folder);
				folderMapping.put(folder.getAbsolutePath(), remoteId);
			}
		});

		// Save folder mapping
		PrintWriter folderFile = new PrintWriter(new FileOutputStream("upload.folder"));
		folderMapping.entrySet().forEach((Entry<String, String> e) -> {
			folderFile.println(MessageFormat.format("{0}:{1}", e.getKey(), e.getValue()));
		});
		folderFile.close();

		// Start uploading files
		PrintWriter uploaded = new PrintWriter(new FileOutputStream("upload.done"));

		AtomicInteger counter = new AtomicInteger(0);
		files.forEach((java.io.File f) -> {
			String parentId = folderMapping.get(f.getParentFile().getAbsolutePath());
			try {
				upload(parentId, f);
				uploaded.println(f.getAbsolutePath());
				uploaded.flush();
				counter.incrementAndGet();
			} catch (Exception e) {
				logger.error("Error uploading file " + f.getAbsolutePath(), e);
			}
		});
		uploaded.close();

		if (counter.get() < files.size()) {
			System.out.println("Not all files are uploaded, run ``resume'' to retry");
		}
	}

	protected void iterate(java.io.File root, java.util.List<java.io.File> files,
			java.util.List<java.io.File> folders) {
		if (root.isDirectory()) {
			folders.add(root);
			for (java.io.File f : root.listFiles()) {
				iterate(f, files, folders);
			}
		} else {
			files.add(root);
		}
	}

	protected String mkdir(String parentId, java.io.File local) {
		File content = new File();
		content.setTitle(local.getName());
		// Set Parent
		List<ParentReference> parents = new ArrayList<ParentReference>();
		ParentReference parent = new ParentReference();
		parent.setId(parentId);
		parents.add(parent);
		content.setParents(parents);

		content.setMimeType(Constants.TYPE_FOLDER);
		try {
			File remote = shell.getDrive().files().insert(content).execute();
			return remote.getId();
		} catch (IOException e) {
			logger.error("Failed to create Folder for " + local.getAbsolutePath(), e);
			throw new RuntimeException(e);
		}
	}

	protected String upload(String parent, java.io.File file) throws Exception {
		return shell.getService().transmit().upload(parent, file);
	}
}
