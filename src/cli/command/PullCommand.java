package cli.command;

import app.AppConfig;
import app.DataNotOnOurNodeException;
import app.Logger;
import app.storage.FileDoesntExistStorageException;

public class PullCommand implements CLICommand {

	@Override
	public String commandName() {
		return "pull";
	}

	@Override
	public void execute(String args) {
		try {
			String []splitArgs = args.split(" ");
			String filePath;
			int version = -1;
			if(splitArgs.length == 2) {
				filePath = splitArgs[0];
				version = Integer.parseInt(splitArgs[1]);
			} else {
				filePath = args;
			}

			AppConfig.chordState.pullFileInWorkDirFromLocalStorage(filePath, version);
			Logger.timestampedStandardPrint("Successfully pulled file " + filePath);
		} catch (NumberFormatException e) {
			Logger.timestampedErrorPrint("Invalid argument for pull: " + args + ". Should be key, which is an int.");
		} catch (FileDoesntExistStorageException e) {
			Logger.timestampedStandardPrint("No such file: " + args);
		} catch (DataNotOnOurNodeException e) {
			Logger.timestampedStandardPrint("Please wait...");
		}
	}

}
