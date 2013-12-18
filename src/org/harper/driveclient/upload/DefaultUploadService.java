package org.harper.driveclient.upload;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.harper.driveclient.Constants;
import org.harper.driveclient.common.DefaultService;
import org.harper.driveclient.common.Extension;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;

public class DefaultUploadService extends DefaultService implements
		UploadService {

	public DefaultUploadService(Drive d) {
		super(d);
	}

	@Override
	public void upload(String remoteFolder, java.io.File localFile)
			throws IOException {
		if (!localFile.exists()) {
			throw new IllegalArgumentException("Local file doesn't exist:"
					+ localFile.getAbsolutePath());
		}

		File file = new File();
		file.setTitle(localFile.getName());
		// Set Parent
		List<ParentReference> parents = new ArrayList<ParentReference>();
		ParentReference parent = new ParentReference();
		parent.setId(remoteFolder);
		parents.add(parent);
		file.setParents(parents);
		if (localFile.isDirectory()) {
			file.setMimeType(Constants.TYPE_FOLDER);
			// Execute
			File inserted = drive.files().insert(file).execute();
			ChildReference child = new ChildReference();
			child.setId(inserted.getId());
			drive.children().insert(remoteFolder, child).execute();
			java.io.File[] localChildren = localFile.listFiles();
			if (localChildren != null) {
				for (java.io.File localChild : localChildren) {
					upload(inserted.getId(), localChild);
				}
			}
		} else {
			// TODO Here we didn't pay attention to google documents, as it
			// should not be created locally. Thus all files being uploaded will
			// be assumed to be local files.

			// Determine the MIME type
			String extension = null;
			int extPoint = localFile.getName().lastIndexOf(".");
			if (extPoint != -1) {
				extension = localFile.getName().substring(extPoint + 1);
				Extension ext = Extension.match(extension);
				if (ext != null)
					file.setMimeType(ext.getType().fullName());
				else {
					logger.warn("Unrecognized Extension:" + extension);
				}
			}
			drive.files()
					.insert(file,
							new FileContent(file.getMimeType(), localFile))
					.execute();
		}
	}
}
