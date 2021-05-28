package cli.command;

import app.AppConfig;
import app.Logger;
import app.SillyGitFile;
import app.SillyGitStorageFile;
import app.git.add.AddResult;
import app.storage.FileAlreadyAddedStorageException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;

public class AddCommand implements CLICommand {

	@Override
	public String commandName() {
		return "add";
	}

	@Override
	public void execute(String args) {
		String[] splitArgs = args.split(" ");

		if (splitArgs.length != 0) {
			String pathToFile = args;

			try {
				Optional<AddResult> addResultOpt = AppConfig.chordState.addFileFromMyWorkDir(pathToFile);

				if (addResultOpt.isEmpty()) {
					Logger.timestampedStandardPrint("Files will be added to the system...");
				} else {
					AddResult addResult = addResultOpt.get();
					Logger.timestampedStandardPrint("Local add completed!");
					Logger.timestampedStandardPrint("Results:");
					Logger.timestampedStandardPrint("Success - " + addResult.getSuccesses().stream().map(SillyGitStorageFile::getPathInStorageDir).collect(Collectors.joining(" ")));
					Logger.timestampedStandardPrint("Failures - " + String.join(" ", addResult.getFailedPaths()));
				}
			}
			catch (FileNotFoundException e) {
				Logger.timestampedErrorPrint("Invalid file path - File doesn't exist: " + e);
			}
		} else {
			Logger.timestampedErrorPrint("Invalid arguments for add");
		}

	}

}
