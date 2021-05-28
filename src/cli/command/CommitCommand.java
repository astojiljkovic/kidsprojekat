package cli.command;

import app.*;
import app.git.commit.CommitResult;
import app.storage.CommitConflictStorageException;
import app.storage.FileAlreadyAddedStorageException;
import app.storage.FileDoesntExistStorageException;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CommitCommand implements CLICommand {

    @Override
    public String commandName() {
        return "commit";
    }

    @Override
    public void execute(String args) {
        String commitPath = args;

//        try {
            Optional<CommitResult> commitResultOpt = AppConfig.chordState.commitFileFromMyWorkDir(commitPath, false);

            if(commitResultOpt.isEmpty()) {
                Logger.timestampedStandardPrint("--- Sent remote commit");
                return;
            }

            CommitResult commitResult = commitResultOpt.get();

            Logger.timestampedStandardPrint("--- Local commit result received:");
            Logger.timestampedStandardPrint("--- Success:");
            for (SillyGitStorageFile sgsf : commitResult.getSuccesses()) {
                Logger.timestampedStandardPrint("" + sgsf.getPathInStorageDir());
            }
            Logger.timestampedStandardPrint("--- Failed with errors:");
            for (String path : commitResult.getFailedPaths()) {
                Logger.timestampedStandardPrint("" + path);
            }
            Logger.timestampedStandardPrint("--- Conflicts for resolution:");
            List<String> confclitPaths = commitResult.getConflicts().stream().map(SillyGitFile::getPathInWorkDir).collect(Collectors.toList());
            for (String confclitPath : confclitPaths) {
                Logger.timestampedStandardPrint("" + confclitPath);
            }
            AppConfig.mergeResolver.addConflictsToResolve(confclitPaths);

//        } catch (FileNotFoundException e) {
//            Logger.timestampedErrorPrint("Cannot commit file - Doesn't exist in my work dir: " + e);
//        } catch (FileAlreadyAddedStorageException e) {
//            Logger.timestampedErrorPrint("Cannot commit file - File with this version already exists in storage: " + e);
//        } catch (FileDoesntExistStorageException e) {
//            Logger.timestampedErrorPrint("Cannot commit file - File must be added to the storage first: " + e);
//        } catch (FileNotAddedFirstCommitException e) {
//            Logger.timestampedErrorPrint("Cannot commit file - File must be added before comitting: " + e);
//        } catch (CommitConflictStorageException e) {
//            Logger.timestampedErrorPrint("Cannot commit file - There was a conflict: " + e);
//            AppConfig.mergeResolver.addConflictToResolve(commitPath);
//        }
    }
}
