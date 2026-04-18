package io.github.connellite.compress;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CompressFileTest {

    @Test
    void compressThenDecompress_roundTrip(@TempDir Path dir) throws IOException {
        byte[] original = "hello gzip world — привет".getBytes(StandardCharsets.UTF_8);
        Path plain = dir.resolve("plain.bin");
        Path gz = dir.resolve("plain.bin.gz");
        Path out = dir.resolve("restored.bin");
        Files.write(plain, original);

        CompressFile.compressGzipFile(plain.toFile(), gz.toFile());
        CompressFile.decompressGzipFile(gz.toFile(), out.toFile());

        assertArrayEquals(original, Files.readAllBytes(out));
    }

    @Test
    void compressGzipFile_nullSourceThrows() {
        assertThrows(NullPointerException.class,
                () -> CompressFile.compressGzipFile(null, new File("x.gz")));
    }

    @Test
    void compressGzipFile_nullDestinationThrows(@TempDir Path dir) throws IOException {
        Path plain = dir.resolve("a.txt");
        Files.writeString(plain, "a");
        assertThrows(NullPointerException.class,
                () -> CompressFile.compressGzipFile(plain.toFile(), null));
    }

    @Test
    void decompressGzipFile_nullGzipThrows() {
        assertThrows(NullPointerException.class,
                () -> CompressFile.decompressGzipFile(null, new File("out")));
    }

    @Test
    void decompressGzipFile_nullOutputThrows(@TempDir Path dir) throws IOException {
        Path gz = dir.resolve("x.gz");
        Files.writeString(dir.resolve("src.txt"), "z");
        CompressFile.compressGzipFile(dir.resolve("src.txt").toFile(), gz.toFile());
        assertThrows(NullPointerException.class,
                () -> CompressFile.decompressGzipFile(gz.toFile(), null));
    }
}
