package io.github.connellite.util.internal;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;

/**
 * ASCII hex digits: nibble lookup for {@code 0-9}, {@code a-f}, {@code A-F}, and lowercase encoding.
 */
@UtilityClass
public class HexDigits {

    private static final char[] LOWER_HEX = "0123456789abcdef".toCharArray();

    private static final byte[] HEX_VALUES;

    static {
        byte[] hexValues = new byte[128];
        Arrays.fill(hexValues, (byte) -1);
        for (int i = 0; i < 10; i++) {
            hexValues['0' + i] = (byte) i;
        }
        for (int i = 0; i < 6; i++) {
            hexValues['a' + i] = (byte) (i + 10);
            hexValues['A' + i] = (byte) (i + 10);
        }
        HEX_VALUES = hexValues;
    }

    /**
     * Returns the hex value of {@code code}, or {@code -1} when it is not an ASCII hex digit.
     */
    public static int hexValue(int code) {
        if (code < 0 || code >= HEX_VALUES.length) {
            return -1;
        }
        return HEX_VALUES[code];
    }

    /**
     * Appends {@code value} as 2 lowercase hex digits.
     */
    public static void appendHex(Appendable out, byte value) {
        appendDigits(out, value & 0xFF, 2);
    }

    /**
     * Appends {@code value} as 4 lowercase hex digits.
     */
    public static void appendHex(Appendable out, short value) {
        appendDigits(out, value & 0xFFFF, 4);
    }

    /**
     * Appends {@code value} as 8 lowercase hex digits.
     */
    public static void appendHex(Appendable out, int value) {
        appendDigits(out, value & 0xFFFFFFFFL, 8);
    }

    /**
     * Appends {@code value} as 16 lowercase hex digits, most significant nibble first.
     */
    public static void appendHex(Appendable out, long value) {
        appendDigits(out, value, 16);
    }

    private static void appendDigits(Appendable out, long value, int digits) {
        try {
            for (int shift = (digits - 2) * 4; shift >= 0; shift -= 8) {
                int unsigned = (int) ((value >>> shift) & 0xFF);
                out.append(LOWER_HEX[unsigned >>> 4]);
                out.append(LOWER_HEX[unsigned & 0x0F]);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Encodes {@code bytes} as lowercase hex, two characters per byte.
     */
    public static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            appendHex(sb, b);
        }
        return sb.toString();
    }

    /**
     * Decodes an even-length ASCII hex string into bytes.
     *
     * @return decoded bytes, or {@code null} if {@code hex} contains a non-hex character
     */
    public static byte[] fromHex(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = hexValue(hex.charAt(i));
            int lo = hexValue(hex.charAt(i + 1));
            if (hi < 0 || lo < 0) {
                return null;
            }
            out[i / 2] = (byte) ((hi << 4) + lo);
        }
        return out;
    }
}
