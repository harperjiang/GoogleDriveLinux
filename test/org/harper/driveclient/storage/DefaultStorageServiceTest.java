package org.harper.driveclient.storage;

import static org.junit.Assert.assertEquals;

import org.harper.driveclient.snapshot.Snapshot;
import org.junit.Test;

public class DefaultStorageServiceTest {

	@Test
	public void testGet() throws InterruptedException {
		DefaultStorageService dss = new DefaultStorageService();
		dss.put("ABC", "KKK");
		Snapshot ss = new Snapshot();
		ss.setName("Good");
		ss.setMd5Checksum("12324343242121442");

		Snapshot child = new Snapshot();
		child.setName("Child");
		child.setMd5Checksum("adsfdsafewewfsd");
		ss.addChild(child);
		dss.put(StorageService.SNAPSHOT, ss);
		dss.store();
	}

	@Test
	public void testPut() {
		DefaultStorageService dss = new DefaultStorageService();
		dss.load();
		Snapshot ss = dss.get(StorageService.SNAPSHOT);
		assertEquals("Good", ss.getName());
		assertEquals(1, ss.getChildren().size());
		assertEquals("Child", ss.getChildren().get(0).getName());
	}
}
