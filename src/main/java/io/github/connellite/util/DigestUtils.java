package io.github.connellite.util;

import io.github.connellite.util.internal.HexDigits;
import lombok.experimental.UtilityClass;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;

/**
 * Digest helpers for streaming input.
 */
@UtilityClass
public class DigestUtils {

    private static final int BUFFER_SIZE = 8192;

    /**
     * Computes a message digest for all bytes from {@code in} using the given algorithm.
     * <p>Does not close {@code in}.</p>
     *
     * @param in        source stream; must not be {@code null}
     * @param algorithm algorithm name understood by {@link MessageDigest} (e.g. {@code MD5}, {@code SHA-256})
     * @return digest bytes
     * @throws IOException              if reading fails
     * @throws NoSuchAlgorithmException if the algorithm is not available
     */
    public static byte[] digest(InputStream in, String algorithm) throws IOException, NoSuchAlgorithmException {
        Objects.requireNonNull(in, "in");
        Objects.requireNonNull(algorithm, "algorithm");
        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] buffer = new byte[BUFFER_SIZE];
        int n;
        while ((n = in.read(buffer)) != -1) {
            md.update(buffer, 0, n);
        }
        return md.digest();
    }

    /**
     * Computes a message digest for {@code data} using the given algorithm.
     *
     * @param data      raw bytes; must not be {@code null}
     * @param algorithm algorithm name understood by {@link MessageDigest} (e.g. {@code MD5}, {@code SHA-256})
     * @return digest bytes
     * @throws NoSuchAlgorithmException if the algorithm is not available
     */
    public static byte[] digest(byte[] data, String algorithm) throws NoSuchAlgorithmException {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(algorithm, "algorithm");
        MessageDigest md = MessageDigest.getInstance(algorithm);
        return md.digest(data);
    }

    /**
     * Computes a hex-encoded message digest for all bytes from {@code in} using the given algorithm.
     * <p>Does not close {@code in}.</p>
     *
     * @param in        source stream; must not be {@code null}
     * @param algorithm algorithm name understood by {@link MessageDigest} (e.g. {@code MD5}, {@code SHA-256})
     * @return lowercase hex string of the digest
     * @throws IOException              if reading fails
     * @throws NoSuchAlgorithmException if the algorithm is not available
     */
    public static String digestHex(InputStream in, String algorithm) throws IOException, NoSuchAlgorithmException {
        Objects.requireNonNull(in, "in");
        Objects.requireNonNull(algorithm, "algorithm");
        return HexDigits.toHex(digest(in, algorithm));
    }

    /**
     * Computes a hex-encoded message digest for {@code data} using the given algorithm.
     *
     * @param data      raw bytes; must not be {@code null}
     * @param algorithm algorithm name understood by {@link MessageDigest} (e.g. {@code MD5}, {@code SHA-256})
     * @return lowercase hex string of the digest
     * @throws NoSuchAlgorithmException if the algorithm is not available
     */
    public static String digestHex(byte[] data, String algorithm) throws NoSuchAlgorithmException {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(algorithm, "algorithm");
        return HexDigits.toHex(digest(data, algorithm));
    }

    /**
     * Convenience method for computing an MD5 digest.
     */
    public static byte[] md5(InputStream in) throws IOException, NoSuchAlgorithmException {
        Objects.requireNonNull(in, "in");
        return digest(in, "MD5");
    }

    /**
     * Convenience method for computing an MD5 digest.
     */
    public static byte[] md5(byte[] data) throws NoSuchAlgorithmException {
        Objects.requireNonNull(data, "data");
        return digest(data, "MD5");
    }

    /**
     * Convenience method for computing an MD5 digest as lowercase hex string.
     */
    public static String md5Hex(InputStream in) throws IOException, NoSuchAlgorithmException {
        Objects.requireNonNull(in, "in");
        return digestHex(in, "MD5");
    }

    /**
     * Convenience method for computing an MD5 digest as lowercase hex string.
     */
    public static String md5Hex(byte[] data) throws NoSuchAlgorithmException {
        Objects.requireNonNull(data, "data");
        return digestHex(data, "MD5");
    }

    /**
     * Convenience method for computing a SHA-256 digest.
     */
    public static byte[] sha256(InputStream in) throws IOException, NoSuchAlgorithmException {
        Objects.requireNonNull(in, "in");
        return digest(in, "SHA-256");
    }

    /**
     * Convenience method for computing a SHA-256 digest.
     */
    public static byte[] sha256(byte[] data) throws NoSuchAlgorithmException {
        Objects.requireNonNull(data, "data");
        return digest(data, "SHA-256");
    }

    /**
     * Convenience method for computing a SHA-256 digest as lowercase hex string.
     */
    public static String sha256Hex(InputStream in) throws IOException, NoSuchAlgorithmException {
        Objects.requireNonNull(in, "in");
        return digestHex(in, "SHA-256");
    }

    /**
     * Convenience method for computing a SHA-256 digest as lowercase hex string.
     */
    public static String sha256Hex(byte[] data) throws NoSuchAlgorithmException {
        Objects.requireNonNull(data, "data");
        return digestHex(data, "SHA-256");
    }

    // region Base64 of raw content

    /**
     * Encodes all bytes from {@code in} as Base64.
     * <p>Does not close {@code in}.</p>
     *
     * @param in source stream; must not be {@code null}
     * @return Base64-encoded bytes
     * @throws IOException if reading fails
     */
    public static byte[] base64(InputStream in) throws IOException {
        Objects.requireNonNull(in, "in");
        return Base64.getEncoder().encode(readAllBytes(in));
    }

    /**
     * Encodes {@code data} as Base64.
     *
     * @param data raw bytes; must not be {@code null}
     * @return Base64-encoded bytes
     */
    public static byte[] base64(byte[] data) {
        Objects.requireNonNull(data, "data");
        return Base64.getEncoder().encode(data);
    }

    /**
     * Encodes all bytes from {@code in} as a Base64 string.
     * <p>Does not close {@code in}.</p>
     *
     * @param in source stream; must not be {@code null}
     * @return Base64 string
     * @throws IOException if reading fails
     */
    public static String base64String(InputStream in) throws IOException {
        Objects.requireNonNull(in, "in");
        return Base64.getEncoder().encodeToString(readAllBytes(in));
    }

    /**
     * Encodes {@code data} as a Base64 string.
     *
     * @param data raw bytes; must not be {@code null}
     * @return Base64 string
     */
    public static String base64String(byte[] data) {
        Objects.requireNonNull(data, "data");
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Decodes a lowercase/uppercase hex string into bytes.
     *
     * @param hex hex string; length must be even
     * @return decoded bytes
     * @throws IllegalArgumentException if the string has odd length or contains non-hex characters
     */
    public static byte[] fromHex(String hex) {
        Objects.requireNonNull(hex, "hex");
        String s = hex.trim();
        int len = s.length();
        if ((len & 1) != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }
        byte[] out = HexDigits.fromHex(s);
        if (out == null) {
            throw new IllegalArgumentException("Invalid hex character in: " + hex);
        }
        return out;
    }

    /**
     * Decodes a Base64-encoded string into bytes.
     *
     * @param base64 Base64 string
     * @return decoded bytes
     * @throws IllegalArgumentException if the input is not valid Base64
     */
    public static byte[] fromBase64(String base64) {
        Objects.requireNonNull(base64, "base64");
        return Base64.getDecoder().decode(base64);
    }

    /**
     * Decodes Base64-encoded bytes into raw bytes.
     *
     * @param base64 Base64-encoded data (typically ASCII)
     * @return decoded bytes
     * @throws IllegalArgumentException if the input is not valid Base64
     */
    public static byte[] fromBase64(byte[] base64) {
        Objects.requireNonNull(base64, "base64");
        return Base64.getDecoder().decode(base64);
    }

    private static byte[] readAllBytes(InputStream in) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int n;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }
}

