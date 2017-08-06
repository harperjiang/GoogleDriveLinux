package org.harper.driveclient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.harper.driveclient.common.StringUtils;
import org.harper.driveclient.shell.Cd;
import org.harper.driveclient.shell.Command;
import org.harper.driveclient.shell.Exit;
import org.harper.driveclient.shell.List;
import org.harper.driveclient.shell.Mkdir;
import org.harper.driveclient.shell.Pwd;
import org.harper.driveclient.shell.Resume;
import org.harper.driveclient.shell.Upload;
import org.slf4j.LoggerFactory;

import com.google.api.services.drive.Drive;

public class Shell {

	private Drive drive = DriveClientFactory.createDrive();

	private Services service = new Services(drive);

	private String currentFolder = Constants.FOLDER_ROOT;

	private Map<String, Object> context = new HashMap<>();

	public Shell() {
		super();
	}

	public void run() throws Exception {
		BufferedReader lineReader = new BufferedReader(new InputStreamReader(System.in));

		String input = null;
		while (true) {
			System.out.print(PROMPT);
			input = lineReader.readLine();

			if (!StringUtils.isEmpty(input)) {
				Command cmd = parse(input);
				if (cmd != null) {
					try {
						cmd.execute(this);
					} catch (Exception e) {
						LoggerFactory.getLogger(getClass()).error("Error executing command", e);
					}
				}
			}
		}
	}

	public Drive getDrive() {
		return drive;
	}

	public Services getService() {
		return service;
	}

	public String getCurrentFolder() {
		return currentFolder;
	}

	public void setCurrentFolder(String currentFolder) {
		this.currentFolder = currentFolder;
	}

	public Map<String, Object> getContext() {
		return context;
	}

	static final String PROMPT = ">";

	public static void main(String[] args) throws Exception {
		Shell shell = new Shell();
		shell.run();
	}

	static Command parse(String input) {
		String[] pieces = input.split("\\s+");
		switch (pieces[0]) {
		case "ls":
			return new List();
		case "exit":
			return new Exit();
		case "pwd":
			return new Pwd();
		case "cd":
			return new Cd(input);
		case "mkdir":
			return new Mkdir(input);
		case "upload":
			return new Upload(input);
		case "resume":
			return new Resume();
		default:
			return null;
		}
	}
}
