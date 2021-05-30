package app.storage;

import app.AppConfig;
import app.FileSystemUtils;
import app.Logger;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static app.AppConfig.deleteDirectoryJava8;

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

//    private final File storageRoot;
//    private final File replicationRoot;

    private final Path storageRoot;// = Path.of("storage");
    private final Path replicationRoot;// = Path.of("replication");

    public Storage(File root) {
        storageRoot = Path.of(root.toString(), "storage");
        replicationRoot = Path.of(root.toString(), "replication");
        try {
            Files.createDirectories(storageRoot);
            Files.createDirectories(replicationRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    synchronized public SillyGitStorageFile add(String path, String content) throws FileAlreadyAddedStorageException {
        try {
            save(storageRoot, path, content, 0);
            return createSillyGitStorageFile(path, content, 0);
        } catch (FileAlreadyExistsException e) {
            throw new FileAlreadyAddedStorageException(path);
        }
    }

    public void addTransferedFiles(List<SillyGitStorageFile> list) {
        saveTransferedFilesRelativeTo(list, storageRoot);
    }

    private void saveTransferedFilesRelativeTo(List<SillyGitStorageFile> list, Path root) {
        for(SillyGitStorageFile file: list) {
            try {
                save(root, file.getPathInStorageDir(), file.getContent(), file.getVersion());
            } catch (FileAlreadyExistsException e) {
                Logger.timestampedStandardPrint("Adding transfered file failed but it's all good - file already exists");
            }
        }
    }

    synchronized public List<SillyGitStorageFile> dumpAllStoredFiles(){
        return getAllStoredFilesRelativeTo(storageRoot);
//        return getAllStoredFilesRelativeTo(Path.of(""));
    }

    synchronized public void storeReplicationData(int nodeId, List<SillyGitStorageFile> allData) {
        Path rootPath = Path.of(replicationRoot.toString(), getReplicationPathForNodeId(nodeId).toString());

        try {
            if (Files.exists(rootPath)) { //If storage folder exists
                Files.walk(rootPath) //Delete it
                        .sorted(Comparator.reverseOrder())
                        .filter(path -> !path.equals(rootPath))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                Logger.timestampedStandardPrint("Issue deleting file in replication (shouldn't really happen)" + e);
                            }
                        });
            }
        } catch (IOException e) {
            Logger.timestampedStandardPrint("Issue deleting folder in replication (shouldn't really happen)" + e);
        }
        saveTransferedFilesRelativeTo(allData, rootPath);
    }

    public void purgeReplicaForNodeId(int nodeId) {
        storeReplicationData(nodeId, Collections.emptyList());
        //Clean up empty dirs
        FileSystemUtils.removeEmptyDirsInPath(replicationRoot);
    }

    synchronized public void consumeReplicaOfNodeId(int nodeId) {
        Path replicationRootPath = Path.of(replicationRoot.toString(), getReplicationPathForNodeId(nodeId).toString());
        List<SillyGitStorageFile> sgfs = getAllStoredFilesRelativeTo(replicationRootPath);
        addTransferedFiles(sgfs);
        storeReplicationData(nodeId, Collections.emptyList());
        //Clean up empty dirs
        FileSystemUtils.removeEmptyDirsInPath(replicationRoot);
    }

    private Path getReplicationPathForNodeId(int nodeId) {
        return Path.of("" + nodeId);
    }

    private void save(Path root, String path, String content, int version) throws FileAlreadyExistsException {
        String versionedFilename = filenameWithVersion(path, version);
        File fileForSillyFile = new File(root.toString(), versionedFilename);

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

    synchronized public SillyGitStorageFile commit(String pathInStorageDir, String content, String versionHash, boolean force) throws FileDoesntExistStorageException, CommitConflictStorageException {
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
            save(storageRoot, pathInStorageDir,  content, newVersion);
            return createSillyGitStorageFile(pathInStorageDir, content, newVersion);
        } catch (FileAlreadyExistsException e) {
            throw new CommitConflictStorageException(pathInStorageDir, newVersion);
        }
    }

    synchronized public GetResult get(String pathInStorageDir, int version) {
        List<SillyGitStorageFile> resultSgfs = new ArrayList<>();
        List<String> failedPaths = new ArrayList<>();
        //dir/bananica.txt -> dir <- WAT DA FUK?
        //dir -> dir
        //bananica -> bananica.txt

        File fileForPotentialFolder = fileForRelativePathToStorageDir(pathInStorageDir);

        if (fileForPotentialFolder.isDirectory()) {
            Path dirPathRelativeToRoot = Path.of(pathInStorageDir);

            System.out.println("Files to retrieve in storage");
            //Get all files in storage, with path relative to it
            List<String> filesToRetrieve = getAllStoredUnversionedFileNamesRelativeToRoot(dirPathRelativeToRoot); //bananica.txt, dir/jogurt.txt (koji su u folderu "fileToGet")

            for(String storedFileName: filesToRetrieve) {
                System.out.println("File " + storedFileName);
                try {
                    String pathToFile = storedFileName; //Paths.get(directoryRoot.toString(), storedFilePath).toString();
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
        int latestStoredVersion = getLatestStoredVersion(Path.of(belongingFolder.toString(), fileName));
        int realVersion;
        if (version == LATEST_STORAGE_FILE_VERSION) {
            realVersion = latestStoredVersion;
        } else {
            realVersion = version;
        }

        if (!strictVersion && version > latestStoredVersion) {
            realVersion = latestStoredVersion;
        }

        String versionedFilename = filenameWithVersion(fileName, realVersion);

        String p = Path.of(belongingFolder.toString(), versionedFilename).toString();
        System.out.println("File to open in getOneFile " + p);
        File fileForSillyFile = fileForRelativePathToStorageDir(p);

        if (!fileForSillyFile.exists()) {
            System.out.println("File doesn't exist");
            throw new FileDoesntExistStorageException(fileName);
        }

        try {
            String content = Files.readString(fileForSillyFile.toPath());
            String smtg = Path.of(belongingFolder.toString(), fileName).normalize().toString();
            System.out.println("File SGSF to be created " + p);
            return createSillyGitStorageFile(smtg, content, realVersion);
        } catch (IOException e) {
            Logger.timestampedErrorPrint("Cannot read a file from storage " + fileForSillyFile.getPath());
            e.printStackTrace();
            throw new UncheckedIOException(e);
        }
    }

    synchronized public List<String> getAllStoredUnversionedFileNamesRelativeToStorageRoot() {
        return getAllStoredUnversionedFileNamesRelativeToRoot(Path.of(""));
    }

    private List<SillyGitStorageFile> getAllStoredFilesRelativeTo(Path folderRelativeToRoot) {
//        File folder = fileForRelativePathToStorageDir(folderRelativeToRoot.toString());
        File folder = new File(folderRelativeToRoot.toString());
        if(!folder.exists()) {
            return Collections.emptyList();
        }
        try {
            return Files.walk(folder.toPath()) //aleksa/xyz/s1_storage/bananica.txt_version_0, /aleksa/xyz/s1_storage/bananica.txt_version_1, /aleksa/xyz/s1_storage/dir/jogurt.txt_version_0
                    .sorted(Comparator.reverseOrder())
                    .filter(path -> !Files.isDirectory(path))
                    .map(path -> { // bananica.txt_version_0, bananica.txt_version_1, dir/jogurt.txt_version_0
                        return folder.toPath().relativize(path).toString();
                    }).map(s -> {
//                        Path pathForWorkingWithFileSystem = fileForRelativePathToStorageDir(s).toPath();
                        Path pathForWorkingWithFileSystem = Path.of(folderRelativeToRoot.toString(), s);
                        String content = null;
                        try {
                            content = Files.readString(pathForWorkingWithFileSystem);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                        String fileName = filenameFromVersionedFilename(s);
                        int version = versionFromVersionedFilename(s);

                        return createSillyGitStorageFile(fileName, content, version);
                    })

                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<String> getAllUnversionedStoredFilesRelativeTo(Path folderRelativeToRoot) {
        File folder = fileForRelativePathToStorageDir(folderRelativeToRoot.toString());
        try {
            return Files.walk(folder.toPath()) //aleksa/xyz/s1_storage/bananica.txt_version_0, /aleksa/xyz/s1_storage/bananica.txt_version_1, /aleksa/xyz/s1_storage/dir/jogurt.txt_version_0
                    .sorted(Comparator.reverseOrder())
                    .filter(path -> !Files.isDirectory(path))
                    .map(path -> { // bananica.txt_version_0, bananica.txt_version_1, dir/jogurt.txt_version_0
                        return folder.toPath().relativize(path).toString();
                    })
                    .filter(path -> { // bananica.txt_version_0, dir/jogurt.txt_version_1
                        return path.endsWith("_0");
                    }).map(path -> { // bananica.txt, dir/jogurt.txt
                        return filenameFromVersionedFilename(path);
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<String> getAllStoredUnversionedFileNamesRelativeToRoot(Path folderRelativeToRoot) {
        return getAllUnversionedStoredFilesRelativeTo(folderRelativeToRoot);
    }

    //Used before transfering files to others
    synchronized public List<SillyGitStorageFile> removeFilesOnRelativePathsReturningGitFiles(List<String> paths) {
        Set<String> requestedDeletePaths = new HashSet<>(paths);
        //paths => dir, dir/dir, dir/bananica.txt, dir/dir/jogurt.txt
//        Path storageRootPath = Path.of("");
        List<SillyGitStorageFile> allFilesInStorageWithRelativePaths = getAllStoredFilesRelativeTo(storageRoot);
        // dir/dir/bananica.txt_version_0, /dir/jogurt.txt_version_0, samofajl.txt_version_0, bananica.txt_version_0

        List<SillyGitStorageFile> sgfs = allFilesInStorageWithRelativePaths
                .stream()
                .filter(sillyGitStorageFile -> {
                    for(String requestedPathToDelete: requestedDeletePaths) {
                        if (sillyGitStorageFile.getPathInStorageDir().startsWith(requestedPathToDelete)) {
                            return true;
                        }
                    }
                    return false;
                }).collect(Collectors.toList());

        System.out.println("allFilesInStorageWithRelativePaths");
        allFilesInStorageWithRelativePaths.forEach(path -> {
            System.out.println("" + path);
        });

        System.out.println("Paths requested for deleting::");
        paths.forEach(path -> {
            System.out.println("" + path);
        });

        System.out.println("Paths for deleting::");
        sgfs.forEach(path -> {
            System.out.println("" + path);
        });

       sgfs.stream()
                .forEach(sillyGitStorageFile -> {
                    try {
                        String pathWithVersion = filenameWithVersion(sillyGitStorageFile.getPathInStorageDir(),sillyGitStorageFile.getVersion());
                        Path pathForWorkingWithFileSystem = fileForRelativePathToStorageDir(pathWithVersion).toPath();

                        Files.delete(pathForWorkingWithFileSystem);

                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });

        //Clean up empty dirs
        FileSystemUtils.removeEmptyDirsInPath(storageRoot);

        return sgfs;
    }

    private int getLatestStoredVersion(Path filePath) throws FileDoesntExistStorageException {

        Path belongingFolder = filePath.getParent();
        if(belongingFolder == null ){
            belongingFolder = Path.of("");
        }
        String fileName = filePath.getFileName().toString();

        File fileForSearchRoot = fileForRelativePathToStorageDir(belongingFolder.toString());
        try {
            return Files.list(fileForSearchRoot.toPath())
                    .map(path -> { // bananica.txt_version_0, bananica.txt_version_1, jogurt.txt
                        return fileForSearchRoot.toPath().relativize(path).toString();
                    }).filter(s -> { // bananica.txt_version_0, bananica.txt_version_1
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

    private File fileForRelativePathToStorageDir(String storagePath) { //bananica.txt_version_0 -> /aleksa/xyz/storage_1/bananica.txt_version_0
        return new File(storageRoot.toString(), storagePath);
    }

    private File fileForRelativePathToReplicationDir(String storagePath) { //bananica.txt_version_0 -> /aleksa/xyz/storage_1/bananica.txt_version_0
        return new File(replicationRoot.toString(), storagePath);
    }
}
