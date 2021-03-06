package org.harper.driveclient.synchronize;

import java.text.MessageFormat;

public class ChangeRecord {

	public static enum Operation {
		LOCAL_INSERT, LOCAL_DELETE, LOCAL_CHANGE, LOCAL_RENAME, REMOTE_INSERT, REMOTE_DELETE, REMOTE_CHANGE, REMOTE_RENAME
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

	public Operation getOperation() {
		return operation;
	}

	public void setOperation(Operation operation) {
		this.operation = operation;
	}

	public String getLocalFile() {
		return localFile;
	}

	public void setLocalFile(String localFile) {
		this.localFile = localFile;
	}

	public String getRemoteFileId() {
		return remoteFileId;
	}

	public void setRemoteFileId(String remoteFileId) {
		this.remoteFileId = remoteFileId;
	}

	public Object[] getContext() {
		return context;
	}

	public <T> T getContext(int index) {
		return (T) context[index];
	}

	public void setContext(Object[] context) {
		this.context = context;
	}

	@Override
	public String toString() {
		return MessageFormat.format("{0}: local {1}, remote {2}",
				operation.name(), localFile, remoteFileId);
	}
}
