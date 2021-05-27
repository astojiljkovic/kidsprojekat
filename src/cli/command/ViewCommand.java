package cli.command;

import app.AppConfig;
import app.Logger;
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



//			try {
// TODO: 25.5.21. implementiraj
//				AppConfig.chordState.addFileFromMyWorkDir(pathToFile);
//			} catch (FileAlreadyAddedStorageException e) {
//				Logger.timestampedErrorPrint("Cannot add file - File already exists: " + e);
//			}
//			catch (FileNotFoundException e) {
//				Logger.timestampedErrorPrint("Invalid file path - File doesn't exist: " + e);
//			}
//		} else {
//			Logger.timestampedErrorPrint("Invalid command for view - " + args);
		}
	}

}
