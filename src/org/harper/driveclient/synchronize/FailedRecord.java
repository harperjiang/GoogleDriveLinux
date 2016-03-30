package org.harper.driveclient.synchronize;

public class FailedRecord {

	public FailedRecord(Operation operation) {
		this.operation = operation;
	}

	private Operation operation;

	private String error;

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public Operation getOperation() {
		return operation;
	}

	public void setOperation(Operation operation) {
		this.operation = operation;

	}

}
