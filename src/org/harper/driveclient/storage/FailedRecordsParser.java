package org.harper.driveclient.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.harper.driveclient.storage.DefaultStorageService.Parser;
import org.harper.driveclient.synchronize.FailedRecord;
import org.harper.driveclient.synchronize.Operation;
import org.harper.driveclient.synchronize.operation.LocalChange;
import org.harper.driveclient.synchronize.operation.LocalDelete;
import org.harper.driveclient.synchronize.operation.LocalInsert;
import org.harper.driveclient.synchronize.operation.LocalRename;
import org.harper.driveclient.synchronize.operation.RemoteChange;
import org.harper.driveclient.synchronize.operation.RemoteDelete;
import org.harper.driveclient.synchronize.operation.RemoteInsert;
import org.harper.driveclient.synchronize.operation.RemoteRename;

public class FailedRecordsParser implements Parser {

	@Override
	public Object parse(Object jsonInput) {
		List<Map<String, Object>> lists = (List<Map<String, Object>>) jsonInput;
		List<FailedRecord> result = new ArrayList<FailedRecord>();
		for (Map<String, Object> rec : lists) {

			Operation opr = null;
			String oprType = (String) rec.get("operation");
			String localFile = (String) rec.get("localFile");
			String remoteField = (String) rec.get("remoteFileId");
			Object[] context = ((List) rec.get("context")).toArray();
			if ("LOCAL_INSERT".equals(oprType)) {
				opr = new LocalInsert(localFile, remoteField, context);
			} else if ("LOCAL_DELETE".equals(oprType)) {
				opr = new LocalDelete(localFile, remoteField, context);

			} else if ("LOCAL_RENAME".equals(oprType)) {
				opr = new LocalRename(localFile, remoteField, context);

			} else if ("LOCAL_CHANGE".equals(oprType)) {
				opr = new LocalChange(localFile, remoteField, context);

			} else if ("REMOTE_INSERT".equals(oprType)) {
				opr = new RemoteInsert(localFile, remoteField, context);

			} else if ("REMOTE_DELETE".equals(oprType)) {
				opr = new RemoteDelete(localFile, remoteField, context);

			} else if ("REMOTE_RENAME".equals(oprType)) {
				opr = new RemoteRename(localFile, remoteField, context);

			} else if ("REMOTE_CHANGE".equals(oprType)) {
				opr = new RemoteChange(localFile, remoteField, context);

			}

			FailedRecord cr = new FailedRecord(opr);
			cr.setError((String) rec.get("error"));
			result.add(cr);
		}
		return result;
	}
}
