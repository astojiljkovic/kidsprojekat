package cli.command;

import app.AppConfig;
import app.DataNotOnOurNodeException;
import app.Logger;
import app.storage.FileDoesntExistException;

public class DHTGetCommand implements CLICommand {

	@Override
	public String commandName() {
		return "dht_get";
	}

	@Override
	public void execute(String args) {
		try {
			String key = args; // TODO: 25.5.21. Remove key 


			String val = AppConfig.chordState.getValueForCLI(key);
			Logger.timestampedStandardPrint(key + ": " + val); //TODO: store into work_dir
//			if (val.equals("-2")) {
//				Logger.timestampedStandardPrint("Please wait...");
//			}
//			else if (val.equals("-1")) {
//				Logger.timestampedStandardPrint("No such key: " + key);
//			}
//			else {
//				Logger.timestampedStandardPrint(key + ": " + val);
//			}
		} catch (NumberFormatException e) {
			Logger.timestampedErrorPrint("Invalid argument for dht_get: " + args + ". Should be key, which is an int.");
		} catch (FileDoesntExistException e) {
			Logger.timestampedStandardPrint("No such file: " + args);
		} catch (DataNotOnOurNodeException e) {
			Logger.timestampedStandardPrint("Please wait...");
		}
	}

}
