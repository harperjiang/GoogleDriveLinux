package org.harper.driveclient.snapshot;

import java.util.ArrayList;
import java.util.List;

public class Snapshot {

	private String md5Checksum;

	private String name;

	private List<Snapshot> children;
	
	public Snapshot() {
		super();
		children = new ArrayList<Snapshot>();
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

	public void setChildren(List<Snapshot> children) {
		this.children = children;
	}

}
