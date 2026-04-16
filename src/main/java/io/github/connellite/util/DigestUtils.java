package io.github.connellite.util;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Digest helpers for streaming input.
 */
@UtilityClass
public class DigestUtils {

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
    public static byte[] digest(@NonNull InputStream in, @NonNull String algorithm) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        try (DigestInputStream dis = new DigestInputStream(in, md)) {
            byte[] buffer = new byte[8192];
            //noinspection StatementWithEmptyBody
            while (dis.read(buffer) != -1) {
                // reading through to update digest
            }
        }
        return md.digest();
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
    public static String digestHex(@NonNull InputStream in, @NonNull String algorithm) throws IOException, NoSuchAlgorithmException {
        return toHex(digest(in, algorithm));
    }

    /**
     * Convenience method for computing an MD5 digest.
     */
    public static byte[] md5(@NonNull InputStream in) throws IOException, NoSuchAlgorithmException {
        return digest(in, "MD5");
    }

    /**
     * Convenience method for computing an MD5 digest as lowercase hex string.
     */
    public static String md5Hex(@NonNull InputStream in) throws IOException, NoSuchAlgorithmException {
        return digestHex(in, "MD5");
    }

    /**
     * Convenience method for computing a SHA-256 digest.
     */
    public static byte[] sha256(@NonNull InputStream in) throws IOException, NoSuchAlgorithmException {
        return digest(in, "SHA-256");
    }

    /**
     * Convenience method for computing a SHA-256 digest as lowercase hex string.
     */
    public static String sha256Hex(@NonNull InputStream in) throws IOException, NoSuchAlgorithmException {
        return digestHex(in, "SHA-256");
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
    public static byte[] base64(@NonNull InputStream in) throws IOException {
        return Base64.getEncoder().encode(readAllBytes(in));
    }

    /**
     * Encodes all bytes from {@code in} as a Base64 string.
     * <p>Does not close {@code in}.</p>
     *
     * @param in source stream; must not be {@code null}
     * @return Base64 string
     * @throws IOException if reading fails
     */
    public static String base64String(@NonNull InputStream in) throws IOException {
        return Base64.getEncoder().encodeToString(readAllBytes(in));
    }

    // endregion

    /**
     * Decodes a lowercase/uppercase hex string into bytes.
     *
     * @param hex hex string; length must be even
     * @return decoded bytes
     * @throws IllegalArgumentException if the string has odd length or contains non-hex characters
     */
    public static byte[] fromHex(@NonNull String hex) {
        String s = hex.trim();
        int len = s.length();
        if ((len & 1) != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(s.charAt(i), 16);
            int lo = Character.digit(s.charAt(i + 1), 16);
            if (hi == -1 || lo == -1) {
                throw new IllegalArgumentException("Invalid hex character in: " + hex);
            }
            out[i / 2] = (byte) ((hi << 4) + lo);
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
    public static byte[] fromBase64(@NonNull String base64) {
        return Base64.getDecoder().decode(base64);
    }

    private static byte[] readAllBytes(InputStream in) throws IOException {
        byte[] buffer = new byte[8192];
        int n;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >>> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}

