package app.storage;

public class CommitConflictStorageException extends StorageException {
    private String path;
    private int version;

    public CommitConflictStorageException(String path, int version) {
        super();
        this.path = path;
        this.version = version;
    }
}
