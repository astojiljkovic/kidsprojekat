package app.storage;

import app.SillyGitFile;

public class FileDoesntExistException extends StorageException {
    private String path;

    public FileDoesntExistException(String path) {
        super();
        this.path = path;
    }

}
