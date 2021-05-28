package app.git.pull;

import app.SillyGitStorageFile;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

public class PullResult implements Serializable {
    private final List<String> failedPaths;
    private final List<SillyGitStorageFile> successes;

    public PullResult(List<String> failedPaths, List<SillyGitStorageFile> successes) {
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
        return "PullResult{S:" + getSuccesses().stream().map(SillyGitStorageFile::getPathInStorageDir).collect(Collectors.joining(" "))
                + "|F:" + String.join(" ", getFailedPaths()) + "}";
    }
}
