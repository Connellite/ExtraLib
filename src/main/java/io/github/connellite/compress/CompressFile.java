package io.github.connellite.compress;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * GZIP compression and decompression for local files.
 */
@UtilityClass
public class CompressFile {

    private static final int BUFFER_SIZE = 8192;

    /**
     * Decompresses a {@code .gz} file into a plain file.
     *
     * @param gzipFile   gzip-compressed source file; must not be {@code null}
     * @param outputFile destination file (created or overwritten); must not be {@code null}
     * @throws IOException if reading or writing fails
     */
    public static void decompressGzipFile(@NonNull File gzipFile, @NonNull File outputFile) throws IOException {
        decompressGzipFile(gzipFile.toPath(), outputFile.toPath());
    }

    /**
     * Decompresses a {@code .gz} file into a plain file.
     *
     * @param gzipFile   gzip-compressed source path; must not be {@code null}
     * @param outputFile destination path (created or overwritten); must not be {@code null}
     * @throws IOException if reading or writing fails
     */
    public static void decompressGzipFile(@NonNull Path gzipFile, @NonNull Path outputFile) throws IOException {
        try (GZIPInputStream gis = new GZIPInputStream(Files.newInputStream(gzipFile));
             var outputStream = Files.newOutputStream(outputFile)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = gis.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
        }
    }

    /**
     * Gzip-compresses a file and writes the result to another file.
     *
     * @param sourceFile plain source file; must not be {@code null}
     * @param gzipFile   destination gzip file (created or overwritten); must not be {@code null}
     * @throws IOException if reading or writing fails
     */
    public static void compressGzipFile(@NonNull File sourceFile, @NonNull File gzipFile) throws IOException {
        compressGzipFile(sourceFile.toPath(), gzipFile.toPath());
    }

    /**
     * Gzip-compresses a file and writes the result to another file.
     *
     * @param sourceFile plain source path; must not be {@code null}
     * @param gzipFile   destination gzip path (created or overwritten); must not be {@code null}
     * @throws IOException if reading or writing fails
     */
    public static void compressGzipFile(@NonNull Path sourceFile, @NonNull Path gzipFile) throws IOException {
        try (var inputStream = Files.newInputStream(sourceFile);
             GZIPOutputStream gzipOS = new GZIPOutputStream(Files.newOutputStream(gzipFile))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                gzipOS.write(buffer, 0, len);
            }
        }
    }
}
