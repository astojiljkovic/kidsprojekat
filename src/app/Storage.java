package app;

import app.storage.CommitConflictStorageException;
import app.storage.FileAlreadyAddedStorageException;
import app.storage.FileDoesntExistStorageException;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

public class Storage {
    public class GetResult {
        private final List<String> failedPaths;
        private final List<SillyGitStorageFile> successes;

        public GetResult(List<String> failedPaths, List<SillyGitStorageFile> successes) {
            this.failedPaths = failedPaths;
            this.successes = successes;
        }

        public List<String> getFailedPaths() {
            return failedPaths;
        }

        public List<SillyGitStorageFile> getSuccesses() {
            return successes;
        }
    }

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
            Files.createDirectories(fileForSillyFile.toPath().getParent());
            Files.writeString(fileForSillyFile.toPath(), content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            Logger.timestampedErrorPrint("Cannot write to storage " + fileForSillyFile.getPath());
            e.printStackTrace();
        }
    }

    public SillyGitStorageFile commit(String pathInStorageDir, String content, String versionHash, boolean force) throws FileDoesntExistStorageException, CommitConflictStorageException {
        //Commit is always file by file -> pathInStorageDir will never be dir -> get() will always return only one file

        GetResult getResult = get(pathInStorageDir, LATEST_STORAGE_FILE_VERSION);
        if (getResult.successes.isEmpty()) {
            throw new FileDoesntExistStorageException(pathInStorageDir);
        }
        SillyGitStorageFile currentFile = getResult.getSuccesses().get(0);

        int newVersion = currentFile.getVersion() + 1;

        if (!force) {
            //If hashes are not equal, there is a conflict
            if (!currentFile.getVersionHash().equals(versionHash)) {
                throw new CommitConflictStorageException(pathInStorageDir, newVersion);
            }

            //If content is equal, and hashes are equal ^, file is the same
            if (currentFile.getContent().equals(content)) {
                return currentFile;
            }
        }

        try {
            save(pathInStorageDir,  content, newVersion);
            return createSillyGitStorageFile(pathInStorageDir, content, newVersion);
        } catch (FileAlreadyExistsException e) {
            throw new CommitConflictStorageException(pathInStorageDir, newVersion);
        }
    }

    public GetResult get(String pathInStorageDir, int version) {
        List<SillyGitStorageFile> resultSgfs = new ArrayList<>();
        List<String> failedPaths = new ArrayList<>();
        //dir/bananica.txt -> dir
        //dir -> dir
        //bananica -> bananica
        Path dirOrFileOnFirstPath = Path.of(pathInStorageDir).getName(0);
        File fileForPotentialFolder = fileForRelativePathToWorkDir(dirOrFileOnFirstPath.toString());

        if (fileForPotentialFolder.isDirectory()) {
            Path dirPathRelativeToRoot = dirOrFileOnFirstPath;
//            List<SillyGitStorageFile> foundFiles = new ArrayList<>();

            List<String> filesToRetrieve;

            Path filePath = Path.of(pathInStorageDir);

            if (filePath.getNameCount() == 2) {
                String specificFile = filePath.getName(1).toString();
                filesToRetrieve = List.of(specificFile);
            } else {
                filesToRetrieve = getAllStoredFilesPaths(dirPathRelativeToRoot); //bananica.txt, jogurt.txt (koji su u folderu "fileToGet")
            }

            for(String storedFileName: filesToRetrieve) {
                try {
                    String pathToFile = storedFileName; //Paths.get(directoryRoot.toString(), storedFilePath).toString();
                    System.out.println("Path to file u foru: " + pathToFile);
                    SillyGitStorageFile sgsf = getOneFile(dirPathRelativeToRoot, pathToFile, version, false);
                    resultSgfs.add(sgsf);
                } catch (FileDoesntExistStorageException e) {
                    failedPaths.add(storedFileName);
                }
            }

            return new GetResult(failedPaths, resultSgfs);
        }

        //Find file in root e.g. bananica.txt
        try {
            resultSgfs.add(getOneFile(Path.of(""), pathInStorageDir, version, true));
            return new GetResult(failedPaths, resultSgfs);
        } catch (FileDoesntExistStorageException e) {
            failedPaths.add(pathInStorageDir);
            return new GetResult(failedPaths, resultSgfs);
        }
    }

    private SillyGitStorageFile getOneFile(Path belongingFolder, String fileName, int version, boolean strictVersion) throws FileDoesntExistStorageException {

        System.out.println("Get one file entry");

        int latestStoredVersion = getLatestStoredVersion(belongingFolder, fileName);
        System.out.println("Latest stored version " + latestStoredVersion);
        int realVersion;
        if (version == LATEST_STORAGE_FILE_VERSION) {
//            realVersion = getLatestStoredVersion(belongingFolder, fileName);
            realVersion = latestStoredVersion;
        } else {
            realVersion = version;
        }

        if (!strictVersion && version > latestStoredVersion) {
            realVersion = latestStoredVersion;
        }

        String versionedFilename = filenameWithVersion(fileName, realVersion);

        File fileForSillyFile = fileForRelativePathToWorkDir(Path.of(belongingFolder.toString(), versionedFilename).toString());

        System.out.println("Get one file: =======");
        System.out.println("VersionedFilename: " + versionedFilename);
        System.out.println("FileFSF: " + fileForSillyFile.getPath());

        if (!fileForSillyFile.exists()) {
            throw new FileDoesntExistStorageException(fileName);
        }


        try {
            String content = Files.readString(fileForSillyFile.toPath());
            return createSillyGitStorageFile(Path.of(belongingFolder.toString(), fileName).normalize().toString(), content, realVersion);
        } catch (IOException e) {
            Logger.timestampedErrorPrint("Cannot read a file from storage " + fileForSillyFile.getPath());
            e.printStackTrace();
            throw new UncheckedIOException(e);
        }
    }

    public List<String> getAllStoredFilesPaths() {
        return getAllStoredFilesPaths(Path.of(""));
    }

    private List<String> getAllStoredFilesPaths(Path folderRelativeToRoot) {
        File folder = fileForRelativePathToWorkDir(folderRelativeToRoot.toString());
        try {
            return Files.list(folder.toPath()) // /aleksa/xyz/s1_storage/bananica.txt_version_0, /aleksa/xyz/s1_storage/bananica.txt_version_1, /aleksa/xyz/s1_storage/jogurt.txt
                    .map(path -> { // bananica.txt_version_0, bananica.txt_version_1, jogurt.txt
                        return folder.toPath().relativize(path).toString();
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

    private int getLatestStoredVersion(Path searchRoot, String fileName) throws FileDoesntExistStorageException {
        System.out.println("Get latest ver " + fileName);
        File fileForSearchRoot = fileForRelativePathToWorkDir(searchRoot.toString());
        try {
            return Files.list(fileForSearchRoot.toPath())
                    .map(path -> { // bananica.txt_version_0, bananica.txt_version_1, jogurt.txt
                        System.out.println("In map " + path);
                        return fileForSearchRoot.toPath().relativize(path).toString();
                    }).filter(s -> { // bananica.txt_version_0, bananica.txt_version_1
                        System.out.println("In filter " + s);
                        String filename = filenameFromVersionedFilename(s);
                        return filename.equals(fileName);
                    })
                    .map(this::versionFromVersionedFilename)
                    .max(Integer::compareTo).orElseThrow();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (NoSuchElementException e) {
            throw new FileDoesntExistStorageException(fileName);
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
