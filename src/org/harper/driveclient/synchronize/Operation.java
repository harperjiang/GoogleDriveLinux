package org.harper.driveclient.synchronize;

import java.io.IOException;

import org.harper.driveclient.snapshot.Snapshot;

public interface Operation {

	public void execute() throws IOException;

	public void updateSnapshot(Snapshot root);

	public String getName();

	public String getLocalFile();

	public String getRemoteFileId();

	public Object[] getContext();

	public <T> T getContext(int index);
}
