package app;

import java.io.Serializable;

public class SillyGitStorageFile implements Serializable {
    private final String pathInStorageDir;
    private final String content;
    private final int version;
    private final String versionHash;

    public SillyGitStorageFile(String pathInStorageDir, String content, int version, String versionHash) {
        this.pathInStorageDir = pathInStorageDir;
        this.content = content;
        this.version = version;
        this.versionHash = versionHash;
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

    public String getVersionHash() {
        return versionHash;
    }

    @Override
    public String toString() {
        return "SGFS{" + pathInStorageDir + "|" + content + "|" + version + "|" + versionHash + "}";
    }
}
