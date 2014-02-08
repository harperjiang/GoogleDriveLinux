package org.harper.driveclient.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.harper.driveclient.storage.DefaultStorageService.Parser;
import org.harper.driveclient.synchronize.ChangeRecord.Operation;
import org.harper.driveclient.synchronize.FailedRecord;

public class FailedRecordsParser implements Parser {

	@Override
	public Object parse(Object jsonInput) {
		List<Map<String, Object>> lists = (List<Map<String, Object>>) jsonInput;
		List<FailedRecord> result = new ArrayList<FailedRecord>();
		for (Map<String, Object> rec : lists) {
			FailedRecord cr = new FailedRecord(Operation.valueOf((String) rec
					.get("operation")), (String) rec.get("localFile"),
					(String) rec.get("remoteFileId"),
					((List) rec.get("context")).toArray());
			cr.setError((String) rec.get("error"));
			result.add(cr);
		}
		return result;
	}

}
