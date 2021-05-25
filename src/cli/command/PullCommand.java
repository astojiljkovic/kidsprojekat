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
			String key = args; // TODO: 25.5.21. Remove key 


			String val = AppConfig.chordState.getValueForCLI(key);
			Logger.timestampedStandardPrint(key + ": " + val); //TODO: store into work_dir

		} catch (NumberFormatException e) {
			Logger.timestampedErrorPrint("Invalid argument for pull: " + args + ". Should be key, which is an int.");
		} catch (FileDoesntExistException e) {
			Logger.timestampedStandardPrint("No such file: " + args);
		} catch (DataNotOnOurNodeException e) {
			Logger.timestampedStandardPrint("Please wait...");
		}
	}

}
