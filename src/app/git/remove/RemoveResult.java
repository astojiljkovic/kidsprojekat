package app.git.remove;

import app.storage.SillyGitStorageFile;

import java.util.List;
import java.util.stream.Collectors;

public class RemoveResult {
    private final List<String> failedPaths;
    private final List<SillyGitStorageFile> successes;

    public RemoveResult(List<String> failedPaths, List<SillyGitStorageFile> successes) {
        this.failedPaths = failedPaths;
        this.successes = successes;
    }

    public List<String> getFailedPaths() {
        return failedPaths;
    }

    public List<SillyGitStorageFile> getSuccesses() {
        return successes;
    }

    @Override
    public String toString() {
        return "RemoveResult{S:" + getSuccesses().stream().map(SillyGitStorageFile::getPathInStorageDir).collect(Collectors.joining(" "))
                + "|F:" + String.join(" ", getFailedPaths()) + "}";
    }
}
