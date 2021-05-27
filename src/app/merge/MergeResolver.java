package app.merge;

import app.*;
import app.storage.CommitConflictStorageException;
import app.storage.FileAlreadyAddedStorageException;
import app.storage.FileDoesntExistStorageException;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class MergeResolver {
    private MergeState state = MergeState.WAITING_FOR_CONFLICT;

    private List<String> conflictPaths = new ArrayList<>();
    private String resolvingConflictPath;

    private boolean work = true;

    public MergeResolver () {}

    public void addConflictToResolve(String path) {
        conflictPaths.add(path);
        if(state == MergeState.WAITING_FOR_CONFLICT) {
            startConflictResolution();
        }
    }

    private void startConflictResolution() {
        resolvingConflictPath = conflictPaths.get(0);
        conflictPaths.remove(resolvingConflictPath);

        Logger.timestampedStandardPrint("[Merge Resolver] Resolving conflict for path: " + resolvingConflictPath);
        Logger.timestampedStandardPrint("[Merge Resolver] Please enter a command ('view' 'push' 'pull'):");

        state = MergeState.WAITING_FOR_INPUT;
    }

    public void view() throws NotReadyForInputException {
        if (state != MergeState.WAITING_FOR_INPUT) {
            throw new NotReadyForInputException();
        }

        Logger.timestampedStandardPrint("[Merge Resolver] Initiating view for " + resolvingConflictPath);
        try {
            state = MergeState.WAITING_FOR_VIEW;
            AppConfig.chordState.getViewFile(resolvingConflictPath);
//            Logger.timestampedStandardPrint("[Merge Resolver] Temp file successfully fetched.");
//            state = MergeState.WAITING_FOR_INPUT;
            viewResponseReceived(true);
        } catch (FileDoesntExistStorageException e) {
            state = MergeState.WAITING_FOR_INPUT;
            Logger.timestampedErrorPrint("[Merge Resolver] Error resolving conflict - 'view' file should be on our node but not found");
        } catch (DataNotOnOurNodeException e) {
            Logger.timestampedStandardPrint("[Merge Resolver] File is remote. Fetching, please wait...");
        } catch (UnexpectedPullResponseException e) {
            Logger.timestampedErrorPrint("[Merge Resolver] Unexpected pull response.");
            state = MergeState.WAITING_FOR_INPUT;
        }

    }

    public void viewResponseReceived(boolean isSuccess) throws UnexpectedPullResponseException {
        if (state != MergeState.WAITING_FOR_VIEW) {
            throw new UnexpectedPullResponseException();
        }
        state = MergeState.WAITING_FOR_INPUT;
        if (isSuccess) {
            Logger.timestampedStandardPrint("[Merge Resolver] Temp file successfully fetched.");
        } else {
            Logger.timestampedStandardPrint("[Merge Resolver] Couldn't fetch tmp file for " + resolvingConflictPath);
        }
        Logger.timestampedStandardPrint("[Merge Resolver] Please enter a merge command ('view' 'push' 'pull') or 'abort' to abort conflict resolution for: " + resolvingConflictPath);
    }

    public void pull() throws NotReadyForInputException {
        if (state != MergeState.WAITING_FOR_INPUT) {
            throw new NotReadyForInputException();
        }

        Logger.timestampedStandardPrint("[Merge Resolver] Initiating pull for " + resolvingConflictPath);
        state = MergeState.WAITING_FOR_PULL;

        try {
            AppConfig.chordState.pullFileForUs(resolvingConflictPath, -1, PullType.CONFLICT_PULL);
//            Logger.timestampedStandardPrint("[Merge Resolver] Conflict successfully resolved - " + resolvingConflictPath);
            pullResponseReceived(true);
        } catch (FileDoesntExistStorageException e) {
            state = MergeState.WAITING_FOR_INPUT;
            Logger.timestampedErrorPrint("[Merge Resolver] Error resolving conflict - 'pull' file should be on our node but not found");
        } catch (DataNotOnOurNodeException e) {
            Logger.timestampedStandardPrint("[Merge Resolver] File is remote. Pulling, please wait...");
        } catch (UnexpectedPullResponseException e) {
            Logger.timestampedErrorPrint("[Merge Resolver] Unexpected pull response");
            state = MergeState.WAITING_FOR_INPUT;
        }
    }

    public void pullResponseReceived(boolean isSuccess) throws UnexpectedPullResponseException {
        if (state != MergeState.WAITING_FOR_PULL) {
            throw new UnexpectedPullResponseException();
        }
        if (isSuccess) {
            resolveCurrentConflict(false);
        } else {
            state = MergeState.WAITING_FOR_INPUT;
            Logger.timestampedErrorPrint("[Merge Resolver] Couldn't pull to resolve conflict");
        }
    }

    //Push

    public void push() throws NotReadyForInputException {
        if (state != MergeState.WAITING_FOR_INPUT) {
            throw new NotReadyForInputException();
        }

        Logger.timestampedStandardPrint("[Merge Resolver] Initiating push for " + resolvingConflictPath);
        state = MergeState.WAITING_FOR_PUSH;

        try {
            AppConfig.chordState.commitFileFromMyWorkDir(resolvingConflictPath, true);
            pushResponseReceived(true);
        } catch (FileAlreadyAddedStorageException | CommitConflictStorageException e) {
            Logger.timestampedErrorPrint("[Merge Resolver] Error resolving conflict - failed to force commit please try again or abort");
            state = MergeState.WAITING_FOR_INPUT;
        } catch (FileNotAddedFirstCommitException | FileNotFoundException e) {
            Logger.timestampedErrorPrint("[Merge Resolver] Error resolving conflict - trying to push file that was never added");
            state = MergeState.WAITING_FOR_INPUT;
        } catch (FileDoesntExistStorageException e) {
            Logger.timestampedErrorPrint("[Merge Resolver] Error resolving conflict - 'push' file should be in our storage, but not found");
            state = MergeState.WAITING_FOR_INPUT;
        } catch (UnespectedPushResponseException e) {
            Logger.timestampedErrorPrint("[Merge Resolver] Unexpected push response");
            state = MergeState.WAITING_FOR_INPUT;
        }
    }

    public void pushResponseReceived(boolean isSuccess) throws UnespectedPushResponseException {
        if (state != MergeState.WAITING_FOR_PUSH) {
            throw new UnespectedPushResponseException();
        }

        if (isSuccess) {
            resolveCurrentConflict(false);
        } else {
            state = MergeState.WAITING_FOR_INPUT;
            Logger.timestampedErrorPrint("[Merge Resolver] Couldn't force push to resolve conflict");
        }
    }

    private void resolveCurrentConflict(boolean isAborted) {
        if (isAborted) {
            Logger.timestampedStandardPrint("[Merge Resolver] Conflict resolution aborted - " + resolvingConflictPath);
        } else {
            Logger.timestampedStandardPrint("[Merge Resolver] Conflict successfully resolved - " + resolvingConflictPath);
        }
        AppConfig.workDirectory.removeFileForPath(resolvingConflictPath + ".tmp");

        resolvingConflictPath = null;
        state = MergeState.WAITING_FOR_CONFLICT;
        if (conflictPaths.size() > 0) {
            startConflictResolution();
        }
    }

    //Abort

    public void abort() throws NotReadyForInputException {
        if (state != MergeState.WAITING_FOR_INPUT) {
            throw new NotReadyForInputException();
        }
        resolveCurrentConflict(true);
    }

    public boolean isWaitingForInput(){
        return state == MergeState.WAITING_FOR_INPUT;
    }
}
