package app;

import java.io.Serializable;

public class SillyGitFile {
    private final String pathInWorkDir;
    private final String content;

    public SillyGitFile(String pathInWorkDir, String content) {
        this.pathInWorkDir = pathInWorkDir;
        this.content = content;
    }

    public String getPathInWorkDir() {
        return pathInWorkDir;
    }

    public String getContent() {
        return content;
    }
}
