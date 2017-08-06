package org.harper.driveclient.shell;

import org.harper.driveclient.Shell;

public class Mkdir extends Command {

	private String[] inputs;

	public Mkdir(String input) {
		this.inputs = input.split("\\s+");
	}

	@Override
	public void execute(Shell shell) throws Exception {
		if (inputs.length != 2) {
			return;
		}
		FileOprs.mkdir(shell.getDrive(), shell.getCurrentFolder(), inputs[1]);
	}

}
