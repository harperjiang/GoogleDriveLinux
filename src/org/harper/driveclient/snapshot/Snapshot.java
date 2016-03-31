package org.harper.driveclient.snapshot;

import java.io.File;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.harper.driveclient.common.DriveUtils;

public class Snapshot implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6531606229189120802L;

	private String md5Checksum;

	private String name;

	private boolean file;

	private List<Snapshot> children;

	private boolean dirty;

	private transient Snapshot parent;

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
		child.setParent(this);
	}

	public void setChildren(List<Snapshot> children) {
		this.children = children;
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
		if (dirty)
			this.parent.setDirty(true);
	}

	public Snapshot getParent() {
		return parent;
	}

	public void setParent(Snapshot parent) {
		this.parent = parent;
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

	public void update() {
		if (dirty) {
			for (Snapshot child : getChildren()) {
				child.update();
			}
			md5();
			setDirty(false);
		}
	}

	public void make(File root) {
		Snapshot current = this;
		current.setName(DriveUtils.relativePath(root));
		if (root.isDirectory()) {
			current.setFile(false);
			java.io.File[] children = root.listFiles();
			if (children != null) {
				for (java.io.File child : children) {
					Snapshot childsn = new Snapshot();
					childsn.make(child);
					current.addChild(childsn);
				}
			}
			md5();
		} else {
			current.setFile(true);
			current.setMd5Checksum(DriveUtils.md5Checksum(root));
		}

	}

	protected void md5() {
		// Sort children in alphabet order, and calculate the md5 of parent
		Map<String, String> md5s = new HashMap<String, String>();
		PriorityQueue<String> sort = new PriorityQueue<String>();
		for (Snapshot sc : getChildren()) {
			md5s.put(sc.getName(), sc.getMd5Checksum());
			sort.offer(sc.getName());
		}
		StringBuilder sb = new StringBuilder();
		while (!sort.isEmpty()) {
			String name = sort.poll();
			sb.append(MessageFormat.format("{0}:{1};", name, md5s.get(name)));
		}
		setMd5Checksum(DriveUtils.md5Checksum(sb.toString()));
	}
}
