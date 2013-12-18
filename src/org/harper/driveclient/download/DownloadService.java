package org.harper.driveclient.download;

import java.io.File;
import java.io.IOException;

public interface DownloadService {

	public void download(String fileId, File localFolder) throws IOException;
}
