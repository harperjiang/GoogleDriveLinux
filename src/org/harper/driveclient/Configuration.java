package org.harper.driveclient;

import java.io.File;

public class Configuration {

	private static File localRoot = new File("/home/harper/GoogleDrive");

	public static File getConfigFolder() {
		return new File(System.getProperty("user.home"), ".google_drive_client");
	}

	public static File getLocalRoot() {
		return localRoot;
	}

	public static void setLocalRoot(File root) {
		localRoot = root;
	}

	public static long getCheckInterval() {
		return 60000;
	}
}
