package org.harper.driveclient.snapshot;

import java.io.File;
import java.io.IOException;

public interface SnapshotService {

	public Snapshot get();

	public Snapshot make() throws IOException;

	public Snapshot make(File root);
}
