package org.harper.driveclient;

import java.io.File;

public class Configuration {

	public static File getConfigFolder() {
		return new File(System.getProperty("user.home"), ".google_drive_client");
	}

	public static String getLocalRoot() {
		return "./output";
	}
}
