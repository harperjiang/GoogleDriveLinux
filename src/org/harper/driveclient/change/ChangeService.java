package org.harper.driveclient.change;

import java.io.File;
import java.util.List;

public interface ChangeService {

	public List<ChangeRecord> compare(File localRoot, String remoteRoot);
}
