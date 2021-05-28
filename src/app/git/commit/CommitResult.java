package app.git.commit;

import app.SillyGitFile;
import app.SillyGitStorageFile;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

public class CommitResult implements Serializable {
    private final List<String> failedPaths;
    private final List<SillyGitStorageFile> successes;
    private final List<SillyGitFile> conflicts;

    public CommitResult(List<String> failedPaths, List<SillyGitStorageFile> successes, List<SillyGitFile> conflicts) {
        this.failedPaths = failedPaths;
        this.successes = successes;
        this.conflicts = conflicts;
    }

    public List<String> getFailedPaths() {
        return failedPaths;
    }

    public List<SillyGitStorageFile> getSuccesses() {
        return successes;
    }

    public List<SillyGitFile> getConflicts() {
        return conflicts;
    }

    @Override
    public String toString() {
        return "CommitResult{S:" + getSuccesses().stream().map(SillyGitStorageFile::getPathInStorageDir).collect(Collectors.joining(" "))
                + "|F:" + String.join(" ", getFailedPaths())
                + "|C:" + getSuccesses().stream().map(SillyGitStorageFile::getPathInStorageDir).collect(Collectors.joining(" ")) + "}";
    }
}
