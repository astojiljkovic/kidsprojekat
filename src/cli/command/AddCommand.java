package cli.command;

import app.AppConfig;
import app.ChordState;

import java.io.File;
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

			File fileToAdd = AppConfig.fileForRelativePathToWorkDir(pathToFile);
			//Users/aleksa/Destkop/kids/chord/s0_work/bananica.txt -> bananica.txt
			//Users/aleksa/Destkop/kids/chord/s0_work/folder1 -> folder1
			
			if (!fileToAdd.exists()) {
				AppConfig.timestampedErrorPrint("Invalid file path - File doesn't exist " + fileToAdd.getAbsolutePath());
				return;
			}

			try {
				if (fileToAdd.isDirectory()) { // TODO: 25.5.21. Resi folder

				} else {

					String fileName = fileToAdd.getName();
					String content = Files.readString(Path.of(fileToAdd.toURI()));

					AppConfig.chordState.addFile(fileName, content);
				}
			} catch (IOException e) {
				AppConfig.timestampedErrorPrint("Problem reading content of file: " + fileToAdd.getAbsolutePath());
			}
			
//			int key = 0;
//			int value = 0;
//			try {
//				key = Integer.parseInt(splitArgs[0]);
//				value = Integer.parseInt(splitArgs[1]);
//
//				if (key < 0 || key >= ChordState.CHORD_SIZE) {
//					throw new NumberFormatException();
//				}
//				if (value < 0) {
//					throw new NumberFormatException();
//				}
//
//				AppConfig.chordState.putValue(key, value);
//			} catch (NumberFormatException e) {
//				AppConfig.timestampedErrorPrint("Invalid key and value pair. Both should be ints. 0 <= key <= " + ChordState.CHORD_SIZE
//						+ ". 0 <= value.");
//			}
		} else {
			AppConfig.timestampedErrorPrint("Invalid arguments for put");
		}

	}

}
