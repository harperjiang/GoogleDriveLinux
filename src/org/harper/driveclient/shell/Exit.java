package org.harper.driveclient.shell;

import org.harper.driveclient.Shell;

public class Exit extends Command {
	@Override
	public void execute(Shell shell) throws Exception {
		System.exit(0);
	}
}
