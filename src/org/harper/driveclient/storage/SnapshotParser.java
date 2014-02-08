package org.harper.driveclient.storage;

import java.util.Map;

import org.harper.driveclient.snapshot.Snapshot;
import org.harper.driveclient.storage.DefaultStorageService.Parser;

public class SnapshotParser implements Parser {

	@Override
	public Object parse(Object jsonInput) {
		return Snapshot.convert((Map<String, Object>)jsonInput);
	}

}
