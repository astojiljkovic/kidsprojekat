package app.storage;

import app.SillyGitFile;

public class FileDoesntExistStorageException extends StorageException {
    private String path;

    public FileDoesntExistStorageException(String path) {
        super();
        this.path = path;
    }

}
