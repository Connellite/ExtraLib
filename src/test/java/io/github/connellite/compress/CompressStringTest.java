package io.github.connellite.compress;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CompressStringTest {

    @Test
    void compressStringRLE_compressesRuns() {
        assertEquals("a4b4", CompressString.compressStringRLE("aaaabbbb"));
        assertEquals("a4b1", CompressString.compressStringRLE("aaaab"));
    }

    @Test
    void compressStringRLE_returnsOriginalWhenNotShorter() {
        assertEquals("abc", CompressString.compressStringRLE("abc"));
    }

    @Test
    void compressStringRLE_returnsOriginalWhenSameLengthAsRle() {
        // "a3b1c2" is also length 6 — no win, original kept
        assertEquals("aaabcc", CompressString.compressStringRLE("aaabcc"));
    }

    @Test
    void compressStringRLE_empty() {
        assertEquals("", CompressString.compressStringRLE(""));
    }

    @Test
    void compress_decompress_roundTrip() throws IOException {
        String original = "deflate — тест";
        byte[] packed = CompressString.compress(original);
        assertEquals(original, CompressString.decompress(packed));
    }

    @Test
    void gzip_ungzip_roundTrip() throws IOException {
        String original = "gzip — привет";
        byte[] gz = CompressString.gzip(original);
        assertEquals(original, CompressString.ungzip(gz));
    }

    @Test
    void getStringDump_gzipBranch() throws IOException {
        String text = "payload";
        byte[] gz = CompressString.gzip(text);
        assertEquals(text, CompressString.getStringDump(gz));
    }

    @Test
    void getStringDump_plainUtf8() throws IOException {
        byte[] raw = "plain".getBytes(StandardCharsets.UTF_8);
        assertEquals("plain", CompressString.getStringDump(raw));
    }

    @Test
    void gzip_producesMagicBytes() throws IOException {
        byte[] gz = CompressString.gzip("x");
        assertArrayEquals(new byte[]{31, (byte) 0x8b}, Arrays.copyOf(gz, 2));
    }

    @Test
    void compress_nullThrows() {
        assertThrows(NullPointerException.class, () -> CompressString.compress(null));
    }

    @Test
    void decompress_nullThrows() {
        assertThrows(NullPointerException.class, () -> CompressString.decompress(null));
    }

    @Test
    void gzip_nullThrows() {
        assertThrows(NullPointerException.class, () -> CompressString.gzip(null));
    }

    @Test
    void ungzip_nullThrows() {
        assertThrows(NullPointerException.class, () -> CompressString.ungzip(null));
    }

    @Test
    void getStringDump_nullThrows() {
        assertThrows(NullPointerException.class, () -> CompressString.getStringDump(null));
    }

    @Test
    void compressStringRLE_nullThrows() {
        assertThrows(NullPointerException.class, () -> CompressString.compressStringRLE(null));
    }
}
