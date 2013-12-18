package org.harper.driveclient.common;

import org.harper.driveclient.Constants;

import com.google.api.services.drive.model.File;

public class DriveUtils {

	public static boolean isDirectory(File input) {
		return Constants.TYPE_FOLDER.equals(input.getMimeType());
	}

	public static boolean isGoogleDoc(File input) {
		return !isDirectory(input)
				&& input.getMimeType().startsWith(Constants.TYPE_GDOCS_PREFIX);
	}
}
