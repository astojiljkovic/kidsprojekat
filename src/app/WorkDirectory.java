package app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

public class WorkDirectory {
    private final Map<String, String> versionHashes = new HashMap<>();

    private final File root;

    public WorkDirectory(File root) {
        this.root = root;
    }

    public SillyGitFile getFileForPath(String path) throws FileNotFoundException {
        File fileToAdd = fileForRelativePathToWorkDir(path);

        if (!fileToAdd.exists()) {
            throw new FileNotFoundException();
        }

        try {
            String content = Files.readString(Path.of(fileToAdd.toURI()));

            if(versionHashes.containsKey(path)) {
                return SillyGitFile.newVersionedFile(path, content, versionHashes.get(path));
            } else {
                return SillyGitFile.newUnversionedFile(path, content);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void addFile(String path, String content, String versionHash) {
        File fileToAdd = fileForRelativePathToWorkDir(path);
        try {
            Files.writeString(fileToAdd.toPath(), content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            if (versionHash != null && !versionHash.isEmpty()) {
                versionHashes.put(path, versionHash);
            }
        } catch (IOException e) {
            //TODO: change to WorkDirectoryException -> Cannot add file
            e.printStackTrace();
            throw new UncheckedIOException(e);
        }
    }

    public void removeFileForPath(String path) {
        File fileToDelete = fileForRelativePathToWorkDir(path);
        fileToDelete.delete();
        versionHashes.remove(path);
    }

    private File fileForRelativePathToWorkDir(String fileName) {
        return new File(root, fileName);
    }
}
