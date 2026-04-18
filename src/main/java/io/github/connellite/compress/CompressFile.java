package io.github.connellite.compress;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
        try (FileInputStream fis = new FileInputStream(gzipFile);
             GZIPInputStream gis = new GZIPInputStream(fis);
             FileOutputStream fos = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = gis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
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
        try (FileInputStream fis = new FileInputStream(sourceFile);
             FileOutputStream fos = new FileOutputStream(gzipFile);
             GZIPOutputStream gzipOS = new GZIPOutputStream(fos)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                gzipOS.write(buffer, 0, len);
            }
        }
    }
}
