package app;

import app.storage.CommitConflictStorageException;
import app.storage.FileAlreadyAddedStorageException;
import app.storage.FileDoesntExistStorageException;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class Storage {
    public static final int LATEST_STORAGE_FILE_VERSION = -1;

    private final File root;

    public Storage(File root) {
        this.root = root;
    }

    public SillyGitStorageFile add(String path, String content) throws FileAlreadyAddedStorageException {
        try {
            save(path, content, 0);
            return createSillyGitStorageFile(path, content, 0);
        } catch (FileAlreadyExistsException e) {
            throw new FileAlreadyAddedStorageException(path);
        }
    }

    private void save(String path, String content, int version) throws FileAlreadyExistsException {
        String versionedFilename = filenameWithVersion(path, version);
        File fileForSillyFile = fileForRelativePathToWorkDir(versionedFilename);

        if (fileForSillyFile.exists()) {
            throw new FileAlreadyExistsException(path);
        }

        try {
            Files.writeString(fileForSillyFile.toPath(), content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            Logger.timestampedErrorPrint("Cannot write to storage " + fileForSillyFile.getPath());
            e.printStackTrace();
        }
    }

    public void commit(String pathInStorageDir, String content, String versionHash) throws FileDoesntExistStorageException, CommitConflictStorageException {
        SillyGitStorageFile currentFile = get(pathInStorageDir, LATEST_STORAGE_FILE_VERSION);

        int newVersion = currentFile.getVersion() + 1;

        //If hashes are not equal, there is a conflict
        if (!currentFile.getVersionHash().equals(versionHash)) {
            throw new CommitConflictStorageException(pathInStorageDir, newVersion);
        }

        //If content is equal, and hashes are equal ^, file is the same
        if (currentFile.getContent().equals(content)) {
            return;
        }

        try {
            save(pathInStorageDir,  content, newVersion);
        } catch (FileAlreadyExistsException e) {
            throw new CommitConflictStorageException(pathInStorageDir, newVersion);
        }
    }

    public SillyGitStorageFile get(String pathInStorageDir, int version) throws FileDoesntExistStorageException {
        int realVersion;
        if (version == LATEST_STORAGE_FILE_VERSION) {
            realVersion = getLatestStoredVersion(pathInStorageDir);
        } else {
            realVersion = version;
        }

        String versionedFilename = filenameWithVersion(pathInStorageDir, realVersion);
        File fileForSillyFile = fileForRelativePathToWorkDir(versionedFilename);

        if (!fileForSillyFile.exists()) {
            throw new FileDoesntExistStorageException(pathInStorageDir);
        }

        try {
            String content = Files.readString(fileForSillyFile.toPath());
            return createSillyGitStorageFile(pathInStorageDir, content, realVersion);
        } catch (IOException e) {
            Logger.timestampedErrorPrint("Cannot read a file from storage " + fileForSillyFile.getPath());
            e.printStackTrace();
            throw new UncheckedIOException(e);
        }
    }

    public List<String> getAllStoredFilesPaths() {
        try {
            return Files.list(root.toPath()) // /aleksa/xyz/s1_storage/bananica.txt_version_0, /aleksa/xyz/s1_storage/bananica.txt_version_1, /aleksa/xyz/s1_storage/jogurt.txt
                    .map(path -> { // bananica.txt_version_0, bananica.txt_version_1, jogurt.txt
                        return root.toPath().relativize(path).toString();
                    })
                    .filter(path -> { // bananica.txt_version_0, jogurt.txt
                        return path.endsWith("_0");
                    })
                    .map(path -> { // bananica.txt, jogurt.txt
                            return filenameFromVersionedFilename(path);
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    //Used before transfering files to others
    public List<SillyGitStorageFile> removeFilesOnRelativePathsReturningGitFiles(List<String> paths) {
        //paths => bananica.txt, jogurt.txt
        try {
            // bananica.txt_version_0, bananica.txt_version_1, jogurt.txt
            List<Path> matchingPaths = Files.list(root.toPath()) // /aleksa/xyz/s1_storage/bananica.txt_version_0, /aleksa/xyz/s1_storage/bananica.txt_version_1, /aleksa/xyz/s1_storage/jogurt.txt
                    .map(path -> { // bananica.txt_version_0, bananica.txt_version_1, jogurt.txt, mucak.txt_version_0, mucak.txt_version_1
                        return root.toPath().relativize(path);
                    })
                    .filter(path -> { // bananica.txt_version_0, bananica.txt_version_1, jogurt.txt
                        String nameWithoutVersion = filenameFromVersionedFilename(path.toString());
                        return paths.contains(nameWithoutVersion);
                    }).collect(Collectors.toList());

            return matchingPaths // bananica.txt_version_0, bananica.txt_version_1, jogurt.txt
                    .stream().map(Path::toString)
                    .map(this::fileForRelativePathToWorkDir)
                    .map(file -> {
                        return file.toPath();
                    })
                    .map(path -> {
                        try {
                            String content = Files.readString(path);
                            String relativePathToStorage = root.toPath().relativize(path).toString();
                            String fileName = filenameFromVersionedFilename(relativePathToStorage);
                            int version = versionFromVersionedFilename(relativePathToStorage);
                            Files.delete(path);
                            return createSillyGitStorageFile(fileName, content, version);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }).collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private int getLatestStoredVersion(String pathInStorageDir) throws FileDoesntExistStorageException {
        try {
            return Files.list(root.toPath())
                    .map(path -> { // bananica.txt_version_0, bananica.txt_version_1, jogurt.txt
                        return root.toPath().relativize(path).toString();
                    }).filter(s -> { // bananica.txt_version_0, bananica.txt_version_1
                        String filename = filenameFromVersionedFilename(s);
                        return filename.equals(pathInStorageDir);
                    })
                    .map(this::versionFromVersionedFilename)
                    .max(Integer::compareTo).orElseThrow();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (NoSuchElementException e) {
            throw new FileDoesntExistStorageException(pathInStorageDir);
        }
    }

    private SillyGitStorageFile createSillyGitStorageFile(String path, String content, int version) {
        try {
            byte []shaBytes = MessageDigest.getInstance("SHA-1").digest((content + version).getBytes());
            String hash = Base64.getEncoder().encodeToString(shaBytes);
            return new SillyGitStorageFile(path, content, version, hash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    private String filenameWithVersion(String fileName, int version) {
        return fileName + "_version_" + version;
    }

    private int versionFromVersionedFilename(String versionedFileName) {
        return Integer.parseInt(versionedFileName.split("_version_")[1]);
    }

    private String filenameFromVersionedFilename(String versionedFileName) {
        return versionedFileName.split("_version_")[0];
    }

    private File fileForRelativePathToWorkDir(String storagePath) { //bananica.txt_version_0 -> /aleksa/xyz/storage_1/bananica.txt_version_0
        return new File(root, storagePath);
    }

}
