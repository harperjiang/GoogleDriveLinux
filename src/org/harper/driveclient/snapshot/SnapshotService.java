package org.harper.driveclient.snapshot;

import java.io.IOException;
import java.util.Map;

public interface SnapshotService {

	public Snapshot get();

	public Snapshot make() throws IOException;

	public Map<String, String> remoteMd5() throws IOException;

	public String localMd5(java.io.File root, Map<String, String> context)
			throws IOException;
}
