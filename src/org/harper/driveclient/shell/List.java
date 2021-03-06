package org.harper.driveclient.shell;

import java.io.IOException;
import java.text.MessageFormat;

import org.harper.driveclient.Shell;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.ChildList;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.model.File;

public class List extends Command {

	public void execute(Shell shell) throws Exception {
		Drive d = shell.getDrive();
		ChildList children = d.children().list(shell.getCurrentFolder()).execute();

		children.getItems().forEach((ChildReference c) -> {
			try {
				File f = d.files().get(c.getId()).execute();
				if (!f.getLabels().getTrashed()) {
					System.out.println(MessageFormat.format("{0}\t{1}", f.getTitle(), f.getCreatedDate()));
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}
}
