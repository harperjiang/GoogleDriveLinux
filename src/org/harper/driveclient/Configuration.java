package org.harper.driveclient;

import java.io.File;

public class Configuration {

	private static File localRoot = new File("/Users/harper/GDriveTest");
	
//	private static File localRoot = new File("/home/harper/GoogleDrive");
	
	public static File getConfigFolder() {
//		return new File(System.getProperty("user.home"), ".google_drive_client");
		return new File(System.getProperty("user.home"), ".gdrive_test");
	}

	public static File getLocalRoot() {
		return localRoot;
	}

	public static void setLocalRoot(File root) {
		localRoot = root;
	}

	public static long getCheckInterval() {
		return 600000;
//		return 10000;
	}
}
