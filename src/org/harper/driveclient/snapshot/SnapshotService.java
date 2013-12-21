package org.harper.driveclient.snapshot;

import java.io.IOException;

public interface SnapshotService {

	public Snapshot get();

	public Snapshot make() throws IOException;

}
