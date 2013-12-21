package org.harper.driveclient.transmit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.harper.driveclient.Configuration;
import org.harper.driveclient.Constants;
import org.harper.driveclient.DriveClientFactory;
import org.harper.driveclient.Services;
import org.harper.driveclient.common.DriveUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.api.client.util.IOUtils;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.model.File;

public class DefaultTransmitServiceTest {

	private static DefaultTransmitService transmitService;

	private static Drive drive;

	private static String testFolderId;

	private String fileId1;

	private String fileId2;

	private String fileId3;

	private static java.io.File originalLocalRoot;

	@BeforeClass
	public static void setup() throws IOException {
		originalLocalRoot = Configuration.getLocalRoot();
		Configuration.setLocalRoot(new java.io.File("./testoutput"));
		drive = DriveClientFactory.createDrive();
		transmitService = new DefaultTransmitService(drive, new Services(drive));
		File testFolder = new File();
		testFolder.setTitle("TransmitServiceTest");
		testFolder.setMimeType(Constants.TYPE_FOLDER);
		testFolderId = drive.files().insert(testFolder).execute().getId();
	}

	@Before
	public void prepareData() throws IOException {
		java.io.File file01 = new java.io.File("testoutput/Test001");
		java.io.File file02 = new java.io.File("testoutput/Test002");
		java.io.File file03 = new java.io.File("testoutput/Test003");
		transmitService.upload(testFolderId, file01);
		transmitService.upload(testFolderId, file02);
		transmitService.upload(testFolderId, file03);

		fileId1 = transmitService.getStub().storage().localToRemote()
				.get(DriveUtils.relativePath(file01));
		fileId2 = transmitService.getStub().storage().localToRemote()
				.get(DriveUtils.relativePath(file02));
		fileId3 = transmitService.getStub().storage().localToRemote()
				.get(DriveUtils.relativePath(file03));
	}

	@Test
	public void testDownload() throws IOException {
		transmitService.download(fileId1, new java.io.File("./testoutput"));
	}

	@Test
	public void testDelete() throws IOException {
		transmitService.delete(fileId2);
		for (ChildReference child : drive.children().list(testFolderId)
				.execute().getItems()) {
			if (child.getId().equals(fileId2))
				fail();
		}
	}

	@Test
	public void testRename() throws IOException {
		transmitService.rename(fileId3, "NewName");
		assertEquals("NewName", drive.files().get(fileId3).execute().getTitle());
	}

	@Test
	public void testUpdate() throws IOException {
		transmitService.update(fileId2, new java.io.File("testoutput/Test003"));
		File file2 = drive.files().get(fileId2).execute();
		File file3 = drive.files().get(fileId3).execute();
		assertEquals(file2.getMd5Checksum(), file3.getMd5Checksum());
	}

	@After
	public void cleanData() throws IOException {
		try {
			drive.files().delete(fileId1).execute();
		} catch (Exception e) {
		}
		try {
			drive.files().delete(fileId2).execute();
		} catch (Exception e) {
		}
		try {
			drive.files().delete(fileId3).execute();
		} catch (Exception e) {
		}
	}

	@AfterClass
	public static void tearDown() throws IOException {
		drive.files().delete(testFolderId).execute();
		Configuration.setLocalRoot(originalLocalRoot);
	}
}
