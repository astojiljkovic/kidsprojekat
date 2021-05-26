package app;

import app.storage.FileAlreadyAddedException;
import app.storage.FileDoesntExistException;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class Storage {
    private final File root;

    public Storage(File root) {
        this.root = root;
    }

    public void add(SillyGitStorageFile sgsf) throws FileAlreadyAddedException {
        String versionedFilename = filenameWithVersion(sgsf.getPathInStorageDir(), sgsf.getVersion());
        File fileForSillyFile = fileForRelativePathToWorkDir(versionedFilename);

        if (fileForSillyFile.exists()) {
            throw new FileAlreadyAddedException(sgsf);
        }

        try {
//            String versionedContent = addVersionToRawContent(sgf.getContent());
            Files.writeString(fileForSillyFile.toPath(), sgsf.getContent(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            Logger.timestampedErrorPrint("Cannot write to storage " + fileForSillyFile.getPath());
            e.printStackTrace();
        }
    }


    //baninca.txt

    //banica.txt/
    //  -> 0 - cokoladna
    //  -> 1 - ledena

    //bananica.txt_0
    //bananica.txt_1

    //bananica.txt - cokoladna

//    private String addVersionToRawContent(String content) {
//        return "0\n" + content;
//    }
//
//    private void incrementVersionOfFile(File file) {
//        try {
//            String versionedContent = Files.readString(file.toPath());
//            //content:
//            //0\n
//            //cokoladna\n
//            //1
//            //ledena
//            String[] contentSplit = versionedContent.split("\n");
//            int currentVersion = Integer.parseInt(contentSplit[0]);
//            contentSplit[0] = "" + (currentVersion + 1);
//
//            versionedContent = String.join("\n", contentSplit);
//            Files.writeString(file.toPath(), versionedContent, StandardOpenOption.TRUNCATE_EXISTING);
//        } catch (IOException e) {
//            Logger.timestampedErrorPrint("Cannot update file version " + file.getPath());
//            throw new UncheckedIOException(e);
//        }
//    }

//    public SillyGitStorageFile get(String pathInStorageDir) throws FileDoesntExistException {
//        int latestVersion = getLatestStoredVersion(pathInStorageDir);
//        return get(pathInStorageDir, latestVersion);
//    }

    public SillyGitStorageFile get(String pathInStorageDir, int version) throws FileDoesntExistException { //TODO: fix version
        int realVersion;
        if (version == -1) {
            realVersion = getLatestStoredVersion(pathInStorageDir);
        } else {
            realVersion = version;
        }

        String versionedFilename = filenameWithVersion(pathInStorageDir, realVersion);
        File fileForSillyFile = fileForRelativePathToWorkDir(versionedFilename);

        if (!fileForSillyFile.exists()) {
            throw new FileDoesntExistException(pathInStorageDir);
        }

        try {
            String content = Files.readString(fileForSillyFile.toPath());
            return new SillyGitStorageFile(pathInStorageDir, content, version);
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
//                        return path.split("_version_")[0];
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
                            return new SillyGitStorageFile(fileName, content, version);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }).collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

//                .map(path -> {
//                    try {
//                        String content = Files.readString(path);
//                        String relativePathToStorage = root.toPath().relativize(path).toString();
//                        Files.delete(path);
//                        return new SillyGitStorageFile(relativePathToStorage, content);
//                    } catch (IOException e) {
//                        throw new UncheckedIOException(e);
//                    }
//                })

//        List<SillyGitStorageFile> gitFiles = paths.stream()
//                // List<String>
//                // Map -> String, a vraca: return File
//                .map(this::fileForRelativePathToWorkDir)
//                .map(file -> { return file.toPath(); })
//                .map(path -> {
//                    try {
//                        String content = Files.readString(path);
//                        String relativePathToStorage = root.toPath().relativize(path).toString();
//                        Files.delete(path);
//                        return new SillyGitFile(relativePathToStorage, content);
//                    } catch (IOException e) {
//                        throw new UncheckedIOException(e);
//                    }
//                }).collect(Collectors.toList());
//        return gitFiles;
    }

    private int getLatestStoredVersion(String pathInStorageDir) throws FileDoesntExistException {
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
            throw new FileDoesntExistException(pathInStorageDir);
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
