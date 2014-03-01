package org.harper.driveclient.snapshot;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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

	public static Snapshot convert(Map<String, Object> content) {
		if (null == content || content.isEmpty()) {
			return null;
		}
		Snapshot s = new Snapshot();
		s.setName((String) content.get("name"));
		s.setMd5Checksum((String) content.get("md5Checksum"));
		s.setFile((Boolean) content.get("file"));
		Collection<Map<String, Object>> children = (Collection<Map<String, Object>>) content
				.get("children");
		for (Map<String, Object> child : children) {
			Snapshot c = convert(child);
			if (null != c) {
				s.addChild(c);
			}
		}
		return s;
	}

	@Override
	public String toString() {
		return MessageFormat.format("{0}:{1}", getName(), getMd5Checksum());
	}

}
