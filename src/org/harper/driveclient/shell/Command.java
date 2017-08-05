package org.harper.driveclient.shell;

import org.harper.driveclient.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Command {

	protected Logger logger = LoggerFactory.getLogger(getClass());
	
	public abstract void execute(Shell shell) throws Exception;
}
