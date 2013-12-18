package org.harper.driveclient.change;

import java.io.File;
import java.text.MessageFormat;

public class ChangeRecord {

	public static final String UPLOAD = "upload";

	public static final String DOWNLOAD = "download";

	private String mode;

	private String remote;

	private File local;

	public ChangeRecord(String mode, File local, String remote) {
		this.mode = mode;
		this.local = local;
		this.remote = remote;
	}

	public void synchronize() {
	
	}

	public String toString() {
		return MessageFormat.format("{0} {1} {2}", mode, local.getName(),
				remote);
	}
}
