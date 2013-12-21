package org.harper.driveclient.transmit;

import java.io.File;
import java.io.IOException;

public interface TransmitService {

	public void upload(String remoteFolder, File localFile) throws IOException;

	public void download(String fileId, File localFolder) throws IOException;

	public void delete(String fileId) throws IOException;

	public void rename(String remoteId, String newName) throws IOException;

	public void update(String remoteFileId, File local) throws IOException;
}
