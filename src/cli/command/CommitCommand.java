package cli.command;

import app.AppConfig;
import app.Logger;
import app.SillyGitFile;
import app.storage.FileAlreadyAddedException;

import java.io.FileNotFoundException;
import java.io.IOException;

public class CommitCommand implements CLICommand {

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
        } else {
            Logger.timestampedErrorPrint("Invalid arguments for add");
        }

    }
}
