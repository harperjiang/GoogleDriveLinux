package org.harper.driveclient.storage;

import static org.junit.Assert.fail;

import org.junit.Test;

public class DefaultStorageServiceTest extends DefaultStorageService {

	@Test
	public void testGet() throws InterruptedException {
		DefaultStorageService dss = new DefaultStorageService();
		dss.put("ABC", "KKK");
		dss.store();
	}

	@Test
	public void testPut() {
		fail("Not yet implemented");
	}

}
