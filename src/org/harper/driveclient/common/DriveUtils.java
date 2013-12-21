package org.harper.driveclient.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.harper.driveclient.Configuration;
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

	public static String md5Checksum(java.io.File localFile) {
		try {
			if (!localFile.isFile())
				throw new IllegalArgumentException("Not a file");
			MessageDigest md5 = MessageDigest.getInstance("md5");
			byte[] buffer = new byte[10000];
			FileInputStream fis = new FileInputStream(localFile);
			int count = 0;
			while ((count = fis.read(buffer)) > 0) {
				md5.update(buffer, 0, count);
			}
			fis.close();
			return md5String(md5.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String md5Checksum(String content) {
		try {
			MessageDigest md5 = MessageDigest.getInstance("md5");
			md5.update(content.getBytes());
			return md5String(md5.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		}
	}

	protected static String md5String(byte[] input) {
		StringBuilder sb = new StringBuilder();
		for (byte i : input) {
			sb.append(String.format("%02x", i));
		}
		return sb.toString();
	}

	public static String relativePath(java.io.File absolute) {
		return absolute.getAbsolutePath().replace(
				Configuration.getLocalRoot().getAbsolutePath(), "");
	}

	public static java.io.File absolutePath(String relativePath) {
		return new java.io.File(Configuration.getLocalRoot().getAbsolutePath()
				+ java.io.File.separator + relativePath);
	}
}
