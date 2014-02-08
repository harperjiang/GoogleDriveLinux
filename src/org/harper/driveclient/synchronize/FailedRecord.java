package org.harper.driveclient.synchronize;

public class FailedRecord extends ChangeRecord {

	public FailedRecord(Operation operation, String localPath,
			String remoteFileId, Object[] context) {
		super(operation, localPath, remoteFileId, context);
	}

	public FailedRecord(ChangeRecord record) {
		this(record.getOperation(), record.getLocalFile(), record
				.getRemoteFileId(), record.getContext());
	}

	private String error;

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

}
