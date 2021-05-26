package app.storage;

import app.SillyGitStorageFile;

public class FileAlreadyAddedException extends StorageException {
    private SillyGitStorageFile sgf;

    public FileAlreadyAddedException(SillyGitStorageFile sgf) {
        super();
        this.sgf = sgf;
    }
}
