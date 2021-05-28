package app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WorkDirectory {
    private final Map<String, String> versionHashes = new HashMap<>();

    private final File root;

    public WorkDirectory(File root) {
        this.root = root;
    }

    public List<SillyGitFile> getFilesForPath(String pathRelativeToWorkDir) throws FileNotFoundException {
        File fileToAdd = fileForRelativePathToWorkDir(pathRelativeToWorkDir);

        if (!fileToAdd.exists()) {
            throw new FileNotFoundException();
        }

        try {
            if (fileToAdd.isDirectory()) {
                return Files.list(fileToAdd.toPath()) // chord/s0_work/dir/bananica...
                        .map(absolutePath -> { // dir/bananica
                            return root.toPath().relativize(absolutePath).toString();
                        })
                        .map(this::getSgf)
                        .collect(Collectors.toList());
            }

            return List.of(getSgf(pathRelativeToWorkDir));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private SillyGitFile getSgf(String pathRelativeToWorkDir) {
        try {
            File f = fileForRelativePathToWorkDir(pathRelativeToWorkDir);
            String content = Files.readString(f.toPath());

            if(versionHashes.containsKey(pathRelativeToWorkDir)) {
                return SillyGitFile.newVersionedFile(pathRelativeToWorkDir, content, versionHashes.get(pathRelativeToWorkDir));
            } else {
                return SillyGitFile.newUnversionedFile(pathRelativeToWorkDir, content);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void addFile(String path, String content, String versionHash) {
        File fileToAdd = fileForRelativePathToWorkDir(path);
        try {
            Files.createDirectories(fileToAdd.toPath().getParent());
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

    //returns chord/s0_work/filename
    //Never use *return* of this to create GitSillyFile / get versionHash key...
    //Use it only for working with file system
    private File fileForRelativePathToWorkDir(String fileName) {
        return new File(root, fileName);
    }
}
