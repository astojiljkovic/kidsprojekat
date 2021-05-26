package app;

import java.io.Serializable;

public class SillyGitStorageFile implements Serializable {
    private final String pathInStorageDir;
    private final String content;
    private final int version;

    public SillyGitStorageFile(String pathInStorageDir, String content, int version) {
        this.pathInStorageDir = pathInStorageDir;
        this.content = content;
        this.version = version;
    }

    public String getPathInStorageDir() {
        return pathInStorageDir;
    }

    public String getContent() {
        return content;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "SGFS{" + pathInStorageDir + "|" + content + "|" + version + "}";

    }
}
