package app.merge;

import app.AppConfig;
import app.ChordState;
import app.Logger;

import java.util.ArrayList;
import java.util.List;

public class MergeResolver {
    private MergeState state = MergeState.WAITING_FOR_CONFLICT;
    private String resolvingConflictPath;

    private List<String> conflictPaths = new ArrayList<>();
    private boolean work = true;

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
        AppConfig.chordState.getViewFile(resolvingConflictPath);
        state = MergeState.WAITING_FOR_VIEW;
    }

    public void pull() throws NotReadyForInputException {
        if (state != MergeState.WAITING_FOR_INPUT) {
            throw new NotReadyForInputException();
        }
    }

    public void push() throws NotReadyForInputException {
        if (state != MergeState.WAITING_FOR_INPUT) {
            throw new NotReadyForInputException();
        }
    }


//    public void stop() {
//        work = true;
//        notify();
//    }
//
//    @Override
//    public void run() {
//        if (conflictPaths.size() > 0) {
//            String conflictPath = conflictPaths.get(0);
//            conflictPaths.remove(conflictPath);
//
//            Logger.timestampedStandardPrint("Resolving conflict for path: " + conflictPath);
//            Logger.timestampedStandardPrint("Please enter a command ('view' 'push' 'pull'):");
//
//
//
//        }
//    }
}
