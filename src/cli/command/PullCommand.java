package cli.command;

import app.AppConfig;
import app.DataNotOnOurNodeException;
import app.Logger;
import app.storage.FileDoesntExistException;

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

			String val = AppConfig.chordState.getValueForCLI(filePath, version);
			Logger.timestampedStandardPrint("Successfully pulled file " + filePath + " : " + val);
		} catch (NumberFormatException e) {
			Logger.timestampedErrorPrint("Invalid argument for pull: " + args + ". Should be key, which is an int.");
		} catch (FileDoesntExistException e) {
			Logger.timestampedStandardPrint("No such file: " + args);
		} catch (DataNotOnOurNodeException e) {
			Logger.timestampedStandardPrint("Please wait...");
		}
	}

}
