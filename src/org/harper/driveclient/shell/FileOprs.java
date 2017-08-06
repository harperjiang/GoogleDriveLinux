package org.harper.driveclient.shell;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.harper.driveclient.Constants;
import org.harper.driveclient.common.Extension;
import org.harper.driveclient.common.MimeType;
import org.harper.driveclient.common.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;

public class FileOprs {

	static Logger logger = LoggerFactory.getLogger(FileOprs.class);

	public static String escape(String input) {
		return input.replaceAll("\\\\\\s+", " ");
	}

	public static String mkdir(Drive drive, String parentId, String name) {
		File content = new File();
		content.setTitle(name);
		// Set Parent
		List<ParentReference> parents = new ArrayList<ParentReference>();
		ParentReference parent = new ParentReference();
		parent.setId(parentId);
		parents.add(parent);
		content.setParents(parents);

		content.setMimeType(Constants.TYPE_FOLDER);
		try {
			File remote = drive.files().insert(content).execute();
			return remote.getId();
		} catch (IOException e) {
			logger.error("Failed to create Folder for " + name, e);
			throw new RuntimeException(e);
		}
	}

	public static String upload(Drive drive, String parentId, java.io.File local) throws Exception {
		if (!local.isFile())
			throw new IllegalArgumentException();
		File file = new File();
		file.setTitle(local.getName());
		// Set Parent
		List<ParentReference> parents = new ArrayList<ParentReference>();
		ParentReference parent = new ParentReference();
		parent.setId(parentId);
		parents.add(parent);
		file.setParents(parents);

		// TODO Here we didn't pay attention to google documents, as they
		// should not be created locally. All files being uploaded will
		// be assumed to be local files.

		// Determine the MIME type
		String extension = null;
		int extPoint = local.getName().lastIndexOf(".");
		if (extPoint != -1) {
			extension = local.getName().substring(extPoint + 1);
			Extension ext = Extension.match(extension);
			if (ext != null) {
				file.setMimeType(ext.getType().fullName());
			}
		}
		if (StringUtils.isEmpty(file.getMimeType())) {
			logger.warn(MessageFormat.format("Unrecognized Extension:{0},use default mimetype", extension));
			file.setMimeType(MimeType.application_octet_stream.fullName());
		}
		File remoteFile = drive.files().insert(file, new FileContent(file.getMimeType(), local)).execute();
		return remoteFile.getId();
	}

}
