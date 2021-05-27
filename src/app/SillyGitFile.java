package app;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Optional;

public class SillyGitFile implements Serializable {
    private final String pathInWorkDir;
    private final String content;

    private final String storageHash;

    private SillyGitFile(String pathInWorkDir, String content, String storageHash) {
        this.pathInWorkDir = pathInWorkDir;
        this.content = content;
        this.storageHash = storageHash;
    }

    public static SillyGitFile newVersionedFile(String pathInWorkDir, String content, String storageHash) {
        return new SillyGitFile(pathInWorkDir, content, storageHash);
    }

    public static SillyGitFile newUnversionedFile(String pathInWorkDir, String content) {
        return new SillyGitFile(pathInWorkDir, content, "");
    }

    public String getPathInWorkDir() {
        return pathInWorkDir;
    }

    public String getContent() {
        return content;
    }

    public Optional<String> getStorageHash() {
        if (storageHash.equals("")) {
            return Optional.empty();
        }
        return Optional.of(storageHash);
    }

    public String getValueToHash() {
        String somethingToHash = Path.of(pathInWorkDir).getName(0).toString();
        System.out.println("Hash test - input: " + pathInWorkDir + " output: " + somethingToHash);
        return somethingToHash;
    }

    @Override
    public String toString() {
        return "SGF{" + pathInWorkDir + "|" + content + "|" + storageHash + "}";
    }
}
