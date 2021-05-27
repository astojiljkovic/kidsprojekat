package app.git.add;

import app.SillyGitFile;
import app.SillyGitStorageFile;

import java.util.ArrayList;
import java.util.List;

public class AddResult {
    private final List<String> failedPaths;
    private final List<SillyGitStorageFile> successes;

    public AddResult(List<String> failedPaths, List<SillyGitStorageFile> successes) {
        this.failedPaths = failedPaths;
        this.successes = successes;
    }

    public List<String> getFailedPaths() {
        return failedPaths;
    }

    public List<SillyGitStorageFile> getSuccesses() {
        return successes;
    }
}
