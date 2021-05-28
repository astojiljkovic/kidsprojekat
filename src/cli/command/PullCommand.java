package cli.command;

import app.*;
import app.git.pull.PullResult;
import app.merge.NotReadyForInputException;
import app.storage.FileDoesntExistStorageException;

import java.util.Optional;

public class PullCommand implements CLICommand {

	@Override
	public String commandName() {
		return "pull";
	}

	@Override
	public void execute(String args) {
		try {
			if (args == null) { //Conflict resolution pull  |  `pull`
				AppConfig.mergeResolver.pull();
			} else { //Pull file from system |  bananica.txt   or   bananica.txt 1
				String []splitArgs = args.split(" ");
				String filePath;
				int version = Storage.LATEST_STORAGE_FILE_VERSION;
				if (splitArgs.length == 2) {
					filePath = splitArgs[0];
					version = Integer.parseInt(splitArgs[1]);
				} else {
					filePath = args;
				}

				Optional<PullResult> pullResult = AppConfig.chordState.pullFileForUs(filePath, version, PullType.PULL);

				if (pullResult.isEmpty()) {
					Logger.timestampedStandardPrint("Remote pull initiated for: " + filePath);
				} else {
					Logger.timestampedStandardPrint("Local pull results for: " + filePath);
					Logger.timestampedStandardPrint(pullResult.get().toString());
				}
			}
		} catch (NotReadyForInputException e) {
			Logger.timestampedErrorPrint("Invalid command 'pull' - Merge resolver not ready for input");
		}
	}

}
