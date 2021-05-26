package cli.command;

import app.AppConfig;
import app.FileNotAddedFirstCommitException;
import app.Logger;
import app.storage.CommitConflictStorageException;
import app.storage.FileAlreadyAddedStorageException;
import app.storage.FileDoesntExistStorageException;

import java.io.FileNotFoundException;

public class CommitCommand implements CLICommand {

    @Override
    public String commandName() {
        return "commit";
    }

    @Override
    public void execute(String args) {
        String removePath = args;

        try {
            AppConfig.chordState.commitFileFromMyWorkDir(removePath);
        } catch (FileNotFoundException e) {
            Logger.timestampedErrorPrint("Cannot commit file - Doesn't exist in my work dir: " + e);
        } catch (FileAlreadyAddedStorageException e) {
            Logger.timestampedErrorPrint("Cannot commit file - File with this version already exists in storage: " + e);
        } catch (FileDoesntExistStorageException e) {
            Logger.timestampedErrorPrint("Cannot commit file - File must be added to the storage first: " + e);
        } catch (FileNotAddedFirstCommitException e) {
            Logger.timestampedErrorPrint("Cannot commit file - File must be added before comitting: " + e);
        } catch (CommitConflictStorageException e) {
            Logger.timestampedErrorPrint("Cannot commit file - There was a conflict: " + e);
        }
    }
}
