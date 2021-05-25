package app;

import app.storage.FileAlreadyAddedException;
import app.storage.FileDoesntExistException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class Storage {
    private final File root;

    private List<SillyGitFile> sgfs = new ArrayList<>();

    public Storage(File root) {
        this.root = root;
    }

    public void add(SillyGitFile sgf) throws FileAlreadyAddedException {
        //TODO: dump to file system
        File fileForSillyFile = fileForRelativePathToWorkDir(sgf.getPathInWorkDir());

        boolean exists = sgfs.stream().anyMatch(sillyGitFile -> {
            return sillyGitFile.getPathInWorkDir().equals(sgf.getPathInWorkDir());
        });

        if (exists) {
            throw new FileAlreadyAddedException(sgf);
        }

        sgfs.add(sgf);
    }

    public SillyGitFile get(String pathInWorkDir) throws FileDoesntExistException {
        try {
            return sgfs.stream().filter(sillyGitFile -> {
                return sillyGitFile.getPathInWorkDir().equals(pathInWorkDir);
            }).findFirst().orElseThrow();
        } catch (NoSuchElementException e) {
            throw new FileDoesntExistException(pathInWorkDir);
        }
    }

    public List<SillyGitFile> getAllFiles() { //TODO: Remove
        return sgfs;
    }

    public void setAllFiles(List<SillyGitFile> sgfs) { //TODO: remove
        this.sgfs = sgfs;
    }

    private File fileForRelativePathToWorkDir(String fileName) {
        return new File(root, fileName);
    }
}
