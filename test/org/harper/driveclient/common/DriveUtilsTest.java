package org.harper.driveclient.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DriveUtilsTest {

	@Test
	public void testMd5String() {
		String result = DriveUtils.md5String(new byte[] { (byte)0x3d, (byte)0xb2, (byte)0xc5 });
		assertEquals("3db2c5", result);
	}

}
