package app;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class FileSystemUtils {

    public static void removeEmptyDirsInPath(Path path) {
        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .filter(File::isDirectory)
                    .filter(file -> {
                        return !file.toPath().equals(path);
                    })
                    .forEach(File::delete);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
