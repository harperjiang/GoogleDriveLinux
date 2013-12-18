package org.harper.driveclient.change;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.harper.driveclient.snapshot.Snapshot;

public interface ChangeService {

	public List<ChangeRecord> compare(File localRoot, String remoteRoot,
			Snapshot snapshot) throws IOException;
	
	public Map<String,String> remoteMd5(String remoteRoot) throws IOException;
	
}
