package app.storage;

import java.util.ArrayList;
import java.util.List;

public class FileAlreadyAddedStorageException extends StorageException {
    private final List<String> paths = new ArrayList<>();

    public List<String> getPath() {
        return paths;
    }

    public FileAlreadyAddedStorageException(List<String> paths) {
        super();
        this.paths.addAll(paths);
    }

    public FileAlreadyAddedStorageException(String path) {
        super();
        paths.add(path);
    }
}
