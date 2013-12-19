package org.harper.driveclient.synchronize;

public class ChangeRecord {

	public static enum Operation {
		LOCAL_INSERT, LOCAL_DELETE, LOCAL_CHANGE, LOCAL_RENAME, REMOTE_INSERT, REMOTE_DELETE, REMOTE_CHANGE
	}

	private Operation operation;

	private String localFile;

	private String remoteFileId;

	private Object[] context;

	public ChangeRecord(Operation operation, String localPath,
			String remoteFileId, Object... context) {
		this.operation = operation;
		this.localFile = localPath;
		this.remoteFileId = remoteFileId;
		this.context = context;
	}

	public void synchronize() {

	}

}
