package cli.command;

import app.AppConfig;
import app.Logger;
import app.merge.NotReadyForInputException;
import app.storage.FileAlreadyAddedStorageException;

import java.io.FileNotFoundException;

public class ViewCommand implements CLICommand {

	@Override
	public String commandName() {
		return "view";
	}

	@Override
	public void execute(String args) {
		if (args != null || !args.isEmpty()) {

			try {
				AppConfig.mergeResolver.view();
			} catch (NotReadyForInputException e) {
				Logger.timestampedErrorPrint("Invalid command 'view' - Merge resolver not ready for input");
			}
		} else {
			Logger.timestampedErrorPrint("Invalid command for view - " + args);
		}
	}

}