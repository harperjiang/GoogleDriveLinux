package org.harper.driveclient.shell;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.harper.driveclient.Shell;
import org.harper.driveclient.common.StringUtils;

public class Resume extends Command {

	@Override
	public void execute(Shell shell) throws Exception {
		// Load Folder and reconstruct map
		BufferedReader folderReader = new BufferedReader(new FileReader("upload.folder"));
		Map<String, String> folderMapping = new HashMap<>();
		String line = null;
		while ((line = folderReader.readLine()) != null) {
			String[] pieces = line.split(":");
			folderMapping.put(pieces[0], pieces[1]);
		}
		folderReader.close();

		// Load upload.all and upload.done, compare them and redo the undone.
		BufferedReader allReader = new BufferedReader(new FileReader("upload.all"));
		Set<String> redoFiles = new HashSet<>();

		while ((line = allReader.readLine()) != null) {
			redoFiles.add(line);
		}
		allReader.close();

		BufferedReader doneReader = new BufferedReader(new FileReader("upload.done"));
		while ((line = doneReader.readLine()) != null) {
			redoFiles.remove(line);
		}
		doneReader.close();

		if (redoFiles.isEmpty()) {
			System.out.println("Nothing to resume");
			return;
		}

		PrintWriter doneWriter = new PrintWriter(new BufferedWriter(new FileWriter("upload.done", true)));

		for (String filePath : redoFiles) {
			java.io.File local = new java.io.File(filePath);
			String parentPath = local.getParentFile().getAbsolutePath();
			String parentId = folderMapping.get(parentPath);
			if (StringUtils.isEmpty(parentId)) {
				logger.error("Folder not created " + parentPath);
				System.err.println("File upload failed and cannot resume, please re-execute upload");
				doneWriter.close();
				return;
			} else {
				try {
					System.out.println("Uploading file " + local.getAbsolutePath());
					FileOprs.upload(shell.getDrive(), parentId, local);
					doneWriter.println(local.getAbsolutePath());
					doneWriter.flush();
				} catch (Exception e) {
					logger.error("Error while uploading file " + local.getAbsolutePath(), e);
				}
			}
		}

		doneWriter.close();
	}

}
