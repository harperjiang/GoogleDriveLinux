package org.harper.driveclient.shell;

import org.harper.driveclient.Constants;
import org.harper.driveclient.Shell;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.model.File;

public class Cd extends Command {

	private String[] inputs;

	public Cd(String input) {
		String[] parts = input.split("(?<!\\\\)\\s+");
		inputs = parts;
	}

	@Override
	public void execute(Shell shell) throws Exception {
		if (inputs.length < 1) {
			return;
		}
		switch (inputs[1]) {
		case "..":
			if (!Constants.FOLDER_ROOT.equals(shell.getCurrentFolder())) {
				shell.setCurrentFolder(getParent(shell.getDrive(), shell.getCurrentFolder()));
			}
			break;
		default:
			String newChild = findByName(shell.getDrive(), shell.getCurrentFolder(), inputs[1]);
			if (null != newChild) {
				shell.setCurrentFolder(newChild);
			} else {
				System.err.println("Folder not found:" + inputs[1]);
			}
			break;
		}
	}

	private String getParent(Drive drive, String current) throws Exception {
		return drive.parents().list(current).execute().getItems().get(0).getId();
	}

	private String findByName(Drive drive, String current, String name) throws Exception {
		java.util.List<ChildReference> children = drive.children().list(current).execute().getItems();
		for (ChildReference c : children) {
			File file = drive.files().get(c.getId()).execute();
			if (Constants.TYPE_FOLDER.equals(file.getMimeType()) && name.equals(file.getTitle())) {
				return file.getId();
			}
		}
		return null;
	}

}
