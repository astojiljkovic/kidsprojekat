package cli.command;

import app.AppConfig;
import app.ChordState;
import app.Logger;
import app.SillyGitFile;
import app.storage.FileAlreadyAddedException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

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
				SillyGitFile sgf = AppConfig.workDirectory.getFileForPath(pathToFile);
// TODO: 25.5.21. Resi folder
				AppConfig.chordState.addFile(sgf);
			} catch (FileAlreadyAddedException e) {
				Logger.timestampedErrorPrint("Cannot add file - File already exists: " + e);
			}
			catch (FileNotFoundException e) {
				Logger.timestampedErrorPrint("Invalid file path - File doesn't exist: " + e);
			} catch (IOException e) {
				Logger.timestampedErrorPrint("Problem reading content of file: " + e);
			}

//
//			File fileToAdd = AppConfig.workDirectory.fileForRelativePathToWorkDir(pathToFile);
//			//Users/aleksa/Destkop/kids/chord/s0_work/bananica.txt -> bananica.txt
//			//Users/aleksa/Destkop/kids/chord/s0_work/folder1 -> folder1
//
//			if (!fileToAdd.exists()) {
//				Logger.timestampedErrorPrint("Invalid file path - File doesn't exist " + fileToAdd.getAbsolutePath());
//				return;
//			}
//
//			try {
//				if (fileToAdd.isDirectory()) { // TODO: 25.5.21. Resi folder
//					Logger.timestampedErrorPrint("Invalid file path - Tried to add directory " + fileToAdd.getAbsolutePath());
//				} else {
//
//					String fileName = fileToAdd.getName();
//					String content = Files.readString(Path.of(fileToAdd.toURI()));
//
//					AppConfig.chordState.addFile(fileName, content);
//				}
//			} catch (IOException e) {
//				Logger.timestampedErrorPrint("Problem reading content of file: " + fileToAdd.getAbsolutePath());
//			}
		} else {
			Logger.timestampedErrorPrint("Invalid arguments for add");
		}

	}

}
