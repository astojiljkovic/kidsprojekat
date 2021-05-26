package app.storage;

import app.SillyGitStorageFile;

public class FileAlreadyAddedStorageException extends StorageException {
    private String path;

    public FileAlreadyAddedStorageException(String path) {
        super();
        this.path = path;
    }
}
