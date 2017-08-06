package org.harper.driveclient.shell;

import static org.junit.Assert.*;

import org.junit.Test;

public class FileOprsTest {

	@Test
	public void testEscape() {
		assertEquals("ORT PPT",FileOprs.escape("ORT\\ PPT"));
	}

}
