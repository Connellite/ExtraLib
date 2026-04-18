package io.github.connellite.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSystemUtilsTest {

    @Test
    void deleteRecursively_file_returnsTrueAndRemoves(@TempDir Path dir) throws IOException {
        Path f = dir.resolve("a.txt");
        Files.writeString(f, "x");
        assertTrue(FileSystemUtils.deleteRecursively(f));
        assertFalse(Files.exists(f));
    }

    @Test
    void deleteRecursively_nestedDirectories(@TempDir Path dir) throws IOException {
        Path nested = dir.resolve("a").resolve("b").resolve("c.txt");
        Files.createDirectories(nested.getParent());
        Files.writeString(nested, "d");
        assertTrue(FileSystemUtils.deleteRecursively(dir));
        assertFalse(Files.exists(dir));
    }

    @Test
    void deleteRecursively_missingPath_returnsFalse(@TempDir Path dir) throws IOException {
        Path missing = dir.resolve("nope");
        assertFalse(FileSystemUtils.deleteRecursively(missing));
    }

    @Test
    void deleteRecursively_nullFile_returnsFalse() {
        assertFalse(FileSystemUtils.deleteRecursively((File) null));
    }

    @Test
    void deleteRecursively_nullPath_returnsFalse() throws IOException {
        assertFalse(FileSystemUtils.deleteRecursively((Path) null));
    }

    @Test
    void copyRecursively_singleFile(@TempDir Path dir) throws IOException {
        Path src = dir.resolve("src.txt");
        Path dst = dir.resolve("out.txt");
        Files.writeString(src, "hello");
        FileSystemUtils.copyRecursively(src, dst);
        assertEquals("hello", Files.readString(dst));
    }

    @Test
    void copyRecursively_directoryTree(@TempDir Path dir) throws IOException {
        Path src = dir.resolve("src");
        Files.createDirectories(src.resolve("sub"));
        Files.writeString(src.resolve("root.txt"), "r");
        Files.writeString(src.resolve("sub").resolve("nested.txt"), "n");
        Path dst = dir.resolve("dst");
        FileSystemUtils.copyRecursively(src, dst);
        assertTrue(Files.isDirectory(dst.resolve("sub")));
        assertEquals("r", Files.readString(dst.resolve("root.txt")));
        assertEquals("n", Files.readString(dst.resolve("sub").resolve("nested.txt")));
    }

    @Test
    void copyRecursively_nullSourceThrows() {
        assertThrows(NullPointerException.class, () -> FileSystemUtils.copyRecursively((File) null, new File(".")));
    }

    @Test
    void copyRecursively_nullDestinationThrows(@TempDir Path dir) throws IOException {
        Path src = dir.resolve("s");
        Files.writeString(src, "x");
        assertThrows(NullPointerException.class, () -> FileSystemUtils.copyRecursively(src, (Path) null));
    }

    @Test
    void readAllLines_readsUtf8(@TempDir Path dir) throws IOException {
        Path f = dir.resolve("lines.txt");
        Files.writeString(f, "a\nb\n", StandardCharsets.UTF_8);
        String joined = FileSystemUtils.readAllLines(f).collect(Collectors.joining("|"));
        assertEquals("a|b", joined);
    }

    @Test
    void readAllLines_missingFile_wrapsIOException(@TempDir Path dir) {
        Path missing = dir.resolve("missing.txt");
        assertThrows(RuntimeException.class, () -> FileSystemUtils.readAllLines(missing));
    }
}
