package io.github.connellite.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DigestUtilsTest {

    private InputStream inputOf(String s) {
        return new ByteArrayInputStream(s.getBytes());
    }

    @Test
    void digest_andDigestHex_computeExpectedValues() throws IOException, NoSuchAlgorithmException {
        byte[] md5Bytes = DigestUtils.digest(inputOf("hello"), "MD5");
        byte[] sha256Bytes = DigestUtils.digest(inputOf("hello"), "SHA-256");

        assertEquals("5d41402abc4b2a76b9719d911017c592", bytesToHex(md5Bytes));
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", bytesToHex(sha256Bytes));

        assertEquals("5d41402abc4b2a76b9719d911017c592", DigestUtils.digestHex(inputOf("hello"), "MD5"));
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", DigestUtils.digestHex(inputOf("hello"), "SHA-256"));
    }

    @Test
    void digest_throwsForUnknownAlgorithm() {
        assertThrows(NoSuchAlgorithmException.class, () -> DigestUtils.digest(inputOf("hello"), "NO-SUCH-ALG"));
    }

    @Test
    void md5_andMd5Hex_delegateToDigest() throws IOException, NoSuchAlgorithmException {
        byte[] md5Bytes = DigestUtils.md5(inputOf("hello"));
        String md5Hex = DigestUtils.md5Hex(inputOf("hello"));

        assertEquals("5d41402abc4b2a76b9719d911017c592", bytesToHex(md5Bytes));
        assertEquals("5d41402abc4b2a76b9719d911017c592", md5Hex);
    }

    @Test
    void sha256_andSha256Hex_delegateToDigest() throws IOException, NoSuchAlgorithmException {
        byte[] shaBytes = DigestUtils.sha256(inputOf("hello"));
        String shaHex = DigestUtils.sha256Hex(inputOf("hello"));

        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", bytesToHex(shaBytes));
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", shaHex);
    }

    @Test
    void base64_andBase64String_encodeRawContent() throws IOException {
        byte[] expectedBytes = "hello".getBytes();

        byte[] encoded = DigestUtils.base64(inputOf("hello"));
        String encodedString = DigestUtils.base64String(inputOf("hello"));

        assertEquals("aGVsbG8=", encodedString);
        assertArrayEquals(encodedString.getBytes(), encoded);

        byte[] decoded = DigestUtils.fromBase64(encodedString);
        assertArrayEquals(expectedBytes, decoded);
    }

    @Test
    void fromHex_decodesValidHex() {
        assertArrayEquals(new byte[]{0x00}, DigestUtils.fromHex("00"));
        assertArrayEquals(new byte[]{(byte) 0xff}, DigestUtils.fromHex("ff"));
        assertArrayEquals(new byte[]{0x12, (byte) 0xab}, DigestUtils.fromHex("12ab"));
        assertArrayEquals(new byte[]{0x12, (byte) 0xab}, DigestUtils.fromHex("12AB"));
    }

    @Test
    void fromHex_rejectsOddLengthOrInvalidChars() {
        assertThrows(IllegalArgumentException.class, () -> DigestUtils.fromHex("0"));
        assertThrows(IllegalArgumentException.class, () -> DigestUtils.fromHex("xyz"));
        assertThrows(IllegalArgumentException.class, () -> DigestUtils.fromHex("1g"));
    }

    @Test
    void fromBase64_decodesValidString() {
        byte[] decoded = DigestUtils.fromBase64("aGVsbG8=");
        assertArrayEquals("hello".getBytes(), decoded);
    }

    @Test
    void fromBase64_rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> DigestUtils.fromBase64("not-base64!!"));
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >>> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
