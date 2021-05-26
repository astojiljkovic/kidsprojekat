package app.storage;

import app.SillyGitStorageFile;

public class FileAlreadyAddedStorageException extends StorageException {
    private SillyGitStorageFile sgf;

    public FileAlreadyAddedStorageException(SillyGitStorageFile sgf) {
        super();
        this.sgf = sgf;
    }
}
