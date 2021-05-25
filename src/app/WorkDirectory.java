package app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class WorkDirectory {
    private final File root;

    public WorkDirectory(File root) {
        this.root = root;
    }

    public SillyGitFile getFileForPath(String path) throws IOException {
        File fileToAdd = fileForRelativePathToWorkDir(path);

        if (!fileToAdd.exists()) {
            throw new FileNotFoundException();
        }

        String content = Files.readString(Path.of(fileToAdd.toURI()));

        return new SillyGitFile(path, content);
    }

    private File fileForRelativePathToWorkDir(String fileName) {
        return new File(root, fileName);
    }
}
