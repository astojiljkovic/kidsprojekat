package cli.command;

import app.*;
import app.merge.NotReadyForInputException;
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
			if (splitArgs.length == 2) { //Pull file from system
				String filePath;
				int version = Storage.LATEST_STORAGE_FILE_VERSION;
				if (splitArgs.length == 2) {
					filePath = splitArgs[0];
					version = Integer.parseInt(splitArgs[1]);
				} else {
					filePath = args;
				}

				AppConfig.chordState.pullFileForUs(filePath, version, PullType.PULL);

				Logger.timestampedStandardPrint("Successfully pulled file " + filePath);
			} else { //Conflict resolution pull
				AppConfig.mergeResolver.pull();
			}
		} catch (FileDoesntExistStorageException e) {
			Logger.timestampedStandardPrint("No such file: " + args);
		} catch (DataNotOnOurNodeException e) {
			Logger.timestampedStandardPrint("Please wait...");
		} catch (NotReadyForInputException e) {
			Logger.timestampedErrorPrint("Invalid command 'view' - Merge resolver not ready for input");
		}
	}

}
