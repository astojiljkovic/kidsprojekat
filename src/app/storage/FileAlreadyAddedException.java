package app.storage;

import app.SillyGitFile;

public class FileAlreadyAddedException extends StorageException {
    private SillyGitFile sgf;

    public FileAlreadyAddedException(SillyGitFile sgf) {
        super();
        this.sgf = sgf;
    }
}
