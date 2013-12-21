package org.harper.driveclient;

import java.io.File;

public class Configuration {

	private static File localRoot = new File("./output");

	public static File getConfigFolder() {
		return new File(System.getProperty("user.home"), ".google_drive_client");
	}

	public static File getLocalRoot() {
		return localRoot;
	}

	public static void setLocalRoot(File root) {
		localRoot = root;
	}
}
