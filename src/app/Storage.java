package app;

import app.storage.FileAlreadyAddedException;
import app.storage.FileDoesntExistException;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

public class Storage {
    private final File root;

    public Storage(File root) {
        this.root = root;
    }

    public void add(SillyGitFile sgf) throws FileAlreadyAddedException {
        File fileForSillyFile = fileForRelativePathToWorkDir(sgf.getPathInWorkDir());

        if (fileForSillyFile.exists()) {
            throw new FileAlreadyAddedException(sgf);
        }

        try {
            String versionedContent = addVersionToRawContent(sgf.getContent());
            Files.writeString(fileForSillyFile.toPath(), versionedContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            Logger.timestampedErrorPrint("Cannot write to storage " + fileForSillyFile.getPath());
            e.printStackTrace();
        }
    }

    private String addVersionToRawContent(String content) {
        return "0\n" + content;
    }

    private void incrementVersionOfFile(File file) {
        try {
            String versionedContent = Files.readString(file.toPath());
            //content:
            //0\n
            //cokoladna\n
            String[] contentSplit = versionedContent.split("\n");
            int currentVersion = Integer.parseInt(contentSplit[0]);
            contentSplit[0] = "" + (currentVersion + 1);

            versionedContent = String.join("\n", contentSplit);
            Files.writeString(file.toPath(), versionedContent, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            Logger.timestampedErrorPrint("Cannot update file version " + file.getPath());
            throw new UncheckedIOException(e);
        }
    }

    public SillyGitFile get(String pathInWorkDir) throws FileDoesntExistException {
        File fileForSillyFile = fileForRelativePathToWorkDir(pathInWorkDir);

        if (!fileForSillyFile.exists()) {
            throw new FileDoesntExistException(pathInWorkDir);
        }

        try {
            String content = Files.readString(fileForSillyFile.toPath());
            return new SillyGitFile(pathInWorkDir, content);
        } catch (IOException e) {
            Logger.timestampedErrorPrint("Cannot read a file from storage " + fileForSillyFile.getPath());
            e.printStackTrace();
            throw new UncheckedIOException(e);
        }
    }

    public List<String> getAllStoredFilesPaths() {
        try {
            return Files.list(root.toPath())
                    .map(path -> {
                        return root.toPath().relativize(path).toString();
                    }).collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    //Used before transfering files to others
    public List<SillyGitFile> removeFilesOnRelativePathsReturningGitFiles(List<String> paths) {
        List<SillyGitFile> gitFiles = paths.stream()
                // List<String>
                // Map -> String, a vraca: return File
                .map(this::fileForRelativePathToWorkDir)
                .map(file -> { return file.toPath(); })
                .map(path -> {
                    try {
                        String content = Files.readString(path);
                        String relativePathToStorage = root.toPath().relativize(path).toString();
                        Files.delete(path);
                        return new SillyGitFile(relativePathToStorage, content);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }).collect(Collectors.toList());
        return gitFiles;
    }

    private File fileForRelativePathToWorkDir(String fileName) {
        return new File(root, fileName);
    }
}
