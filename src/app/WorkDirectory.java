package app;

import java.io.File;

public class WorkDirectory {
    private final File root;

    public WorkDirectory(File root) {
        this.root = root;
    }


    public File fileForRelativePathToWorkDir(String fileName) {
        return new File(root, fileName);
    }
}
