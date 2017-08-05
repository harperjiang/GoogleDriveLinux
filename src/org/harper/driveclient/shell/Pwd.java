package org.harper.driveclient.shell;

import org.harper.driveclient.Shell;

public class Pwd extends Command {
	@Override
	public void execute(Shell shell) throws Exception {
		System.out.println(shell.getCurrentFolder());
	}
}
