package cli.command;

import app.AppConfig;
import app.Logger;
import app.SillyGitFile;
import app.storage.FileAlreadyAddedStorageException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.stream.Collectors;

public class AddCommand implements CLICommand {

	@Override
	public String commandName() {
		return "add";
	}

	@Override
	public void execute(String args) {
		String[] splitArgs = args.split(" ");

		//add bananica.txt
		if (splitArgs.length != 0) {
			String pathToFile = args;

			try {
// TODO: 25.5.21. Resi folder
				AppConfig.chordState.addFileFromMyWorkDir(pathToFile);
			} catch (FileAlreadyAddedStorageException e) {
				Logger.timestampedErrorPrint("Cannot add file - File already exists: " + String.join(" ", e.getPath()));
			}
			catch (FileNotFoundException e) {
				Logger.timestampedErrorPrint("Invalid file path - File doesn't exist: " + e);
			}
		} else {
			Logger.timestampedErrorPrint("Invalid arguments for add");
		}

	}

}
