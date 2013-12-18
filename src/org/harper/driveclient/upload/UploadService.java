package org.harper.driveclient.upload;

import java.io.File;
import java.io.IOException;

public interface UploadService {

	public void upload(String remoteFolder, File localFile) throws IOException;
}
