package org.harper.driveclient.change;

import java.io.File;
import java.util.List;

import org.harper.driveclient.common.DefaultService;

import com.google.api.services.drive.Drive;

public class DefaultChangeService extends DefaultService implements
		ChangeService {

	public DefaultChangeService(Drive d) {
		super(d);
	}

	@Override
	public List<ChangeRecord> compare(File localRoot, String remoteRoot) {
		// TODO Auto-generated method stub
		return null;
	}

}
