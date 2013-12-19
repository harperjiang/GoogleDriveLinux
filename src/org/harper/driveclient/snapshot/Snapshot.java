package org.harper.driveclient.snapshot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Snapshot implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6531606229189120802L;

	private String md5Checksum;

	private String name;

	private boolean file;

	private List<Snapshot> children;

	public Snapshot() {
		super();
		children = new ArrayList<Snapshot>();
	}

	public boolean isFile() {
		return file;
	}

	public void setFile(boolean file) {
		this.file = file;
	}

	public String getMd5Checksum() {
		return md5Checksum;
	}

	public void setMd5Checksum(String md5Checksum) {
		this.md5Checksum = md5Checksum;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Snapshot> getChildren() {
		return children;
	}

	public void addChild(Snapshot child) {
		this.children.add(child);
	}

	public void setChildren(List<Snapshot> children) {
		this.children = children;
	}

}
