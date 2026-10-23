package io.github.connellite.compress;

import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
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
    public static void decompressGzipFile(File gzipFile, File outputFile) throws IOException {
        Objects.requireNonNull(gzipFile, "gzipFile");
        Objects.requireNonNull(outputFile, "outputFile");
        decompressGzipFile(gzipFile.toPath(), outputFile.toPath());
    }

    /**
     * Decompresses a {@code .gz} file into a plain file.
     *
     * @param gzipFile   gzip-compressed source path; must not be {@code null}
     * @param outputFile destination path (created or overwritten); must not be {@code null}
     * @throws IOException if reading or writing fails
     */
    public static void decompressGzipFile(Path gzipFile, Path outputFile) throws IOException {
        Objects.requireNonNull(gzipFile, "gzipFile");
        Objects.requireNonNull(outputFile, "outputFile");
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
    public static void compressGzipFile(File sourceFile, File gzipFile) throws IOException {
        Objects.requireNonNull(sourceFile, "sourceFile");
        Objects.requireNonNull(gzipFile, "gzipFile");
        compressGzipFile(sourceFile.toPath(), gzipFile.toPath());
    }

    /**
     * Gzip-compresses a file and writes the result to another file.
     *
     * @param sourceFile plain source path; must not be {@code null}
     * @param gzipFile   destination gzip path (created or overwritten); must not be {@code null}
     * @throws IOException if reading or writing fails
     */
    public static void compressGzipFile(Path sourceFile, Path gzipFile) throws IOException {
        Objects.requireNonNull(sourceFile, "sourceFile");
        Objects.requireNonNull(gzipFile, "gzipFile");
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
