package org.harper.driveclient.synchronize;

import java.io.IOException;

public interface SynchronizeService {

	public void init() throws IOException;

	public void synchronize() throws IOException;

}
