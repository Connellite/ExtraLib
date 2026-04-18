package io.github.connellite.compress;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Helpers for compressing and decompressing text in memory (raw DEFLATE, GZIP, and simple RLE).
 */
@UtilityClass
public class CompressString {

    private static final int BUFFER_SIZE = 8192;

    /**
     * Run-length encodes consecutive identical characters as {@code char}{@code count}
     * (decimal count). If the encoded form would not be shorter than the input, the original
     * string is returned.
     *
     * @param str source text; must not be {@code null} (empty string is allowed)
     * @return RLE form or the original string when expansion would not save space
     */
    public static String compressStringRLE(@NonNull String str) {
        if (str.isEmpty()) {
            return str;
        }
        int size = countCompression(str);
        if (size >= str.length()) {
            return str;
        }
        StringBuilder out = new StringBuilder(size);
        char last = str.charAt(0);
        int count = 1;
        for (int i = 1; i < str.length(); i++) {
            if (str.charAt(i) == last) {
                count++;
            } else {
                out.append(last);
                out.append(count);
                last = str.charAt(i);
                count = 1;
            }
        }
        out.append(last);
        out.append(count);
        return out.toString();
    }

    private static int countCompression(String str) {
        char last = str.charAt(0);
        int size = 0;
        int count = 1;
        for (int i = 1; i < str.length(); i++) {
            if (str.charAt(i) == last) {
                count++;
            } else {
                size += 1 + String.valueOf(count).length();
                last = str.charAt(i);
                count = 1;
            }
        }
        size += 1 + String.valueOf(count).length();
        return size;
    }

    /**
     * Compresses UTF-8 text with the raw DEFLATE format (zlib wrapper is not used; only deflated payload).
     *
     * @param text plain text; must not be {@code null}
     * @return deflated bytes
     * @throws IOException if compression fails
     */
    public static byte[] compress(@NonNull String text) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DeflaterOutputStream out = new DeflaterOutputStream(baos)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        }
        return baos.toByteArray();
    }

    /**
     * Decompresses bytes produced by {@link #compress(String)} back to a UTF-8 string.
     *
     * @param bytes deflated payload; must not be {@code null}
     * @return restored text
     * @throws IOException if the payload is not valid DEFLATE data
     */
    public static String decompress(@NonNull byte[] bytes) throws IOException {
        try (InflaterInputStream in = new InflaterInputStream(new ByteArrayInputStream(bytes));
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = in.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    /**
     * Decompresses a GZIP-wrapped UTF-8 byte sequence to a string.
     *
     * @param bytes gzip-compressed UTF-8; must not be {@code null}
     * @return decoded text
     * @throws IOException if the input is not valid GZIP
     */
    public static String ungzip(@NonNull byte[] bytes) throws IOException {
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            return new String(gis.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Gzip-compresses a string using UTF-8 encoding.
     *
     * @param s text to compress; must not be {@code null}
     * @return gzip file payload (magic {@code 0x1f 0x8b}, ...})
     * @throws IOException if writing fails
     */
    public static byte[] gzip(@NonNull String s) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bos);
             OutputStreamWriter osw = new OutputStreamWriter(gzip, StandardCharsets.UTF_8)) {
            osw.write(s);
        }
        return bos.toByteArray();
    }

    /**
     * If {@code bytes} look like GZIP ({@code 1f 8b}), decompresses as UTF-8; otherwise decodes
     * the bytes directly as UTF-8.
     *
     * @param bytes raw or gzip-compressed UTF-8; must not be {@code null}
     * @return text interpretation of the payload
     * @throws IOException if gzip decompression fails
     */
    public static String getStringDump(@NonNull byte[] bytes) throws IOException {
        if (isGzipStream(bytes)) {
            return ungzip(bytes);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static boolean isGzipStream(byte[] bytes) {
        return bytes.length >= 2 && bytes[0] == 31 && bytes[1] == (byte) 0x8b;
    }
}
