package app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class WorkDirectory {
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
            return new SillyGitFile(path, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void addFile(SillyGitFile sgf) {
        File fileToAdd = fileForRelativePathToWorkDir(sgf.getPathInWorkDir());
        try {
            Files.writeString(fileToAdd.toPath(), sgf.getContent(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            //TODO: change to WorkDirectoryException -> Cannot add file
            e.printStackTrace();
            throw new UncheckedIOException(e);
        }
    }

    public void removeFileForPath(String path) {
        File fileToDelete = fileForRelativePathToWorkDir(path);
        fileToDelete.delete();
    }

    private File fileForRelativePathToWorkDir(String fileName) {
        return new File(root, fileName);
    }
}
