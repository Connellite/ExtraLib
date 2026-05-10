package io.github.connellite.util;

import lombok.experimental.UtilityClass;

import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

/**
 * Utilities for {@link UUID}: canonical and compact string form, 16-byte binary layout,
 * 32-character hex, conversion from arbitrary {@link Object} shapes, and unsigned 128-bit
 * {@link BigInteger} mapping.
 */
@UtilityClass
public class UuidUtil {

    /**
     * {@code 2^64}; high half multiplier when packing a UUID into a single unsigned 128-bit integer.
     */
    private static final BigInteger B = BigInteger.ONE.shiftLeft(64);

    /**
     * {@link Long#MAX_VALUE}; used when decoding signed {@code long} halves from unsigned big-integer parts.
     */
    private static final BigInteger L = BigInteger.valueOf(Long.MAX_VALUE);

    private static final int[] HEX_VALUES;

    static {
        int[] hexValues = new int[128];
        Arrays.fill(hexValues, -1);
        for (int i = 0; i < 10; i++) {
            hexValues['0' + i] = i;
        }
        for (int i = 0; i < 6; i++) {
            hexValues['a' + i] = i + 10;
            hexValues['A' + i] = i + 10;
        }
        HEX_VALUES = hexValues;
    }

    /**
     * Returns the UUID string without hyphen separators.
     * <p>Example: {@code 550e8400-e29b-41d4-a716-446655440000} {@code ->}
     * {@code 550e8400e29b41d4a716446655440000}.</p>
     *
     * @param uuid the UUID; must not be {@code null}
     * @return 32 lowercase hex characters (implementation follows {@link UUID#toString()})
     */
    public static String compactUuid(UUID uuid) {
        return compactUuid(uuid.toString());
    }

    /**
     * Removes hyphen characters from a canonical UUID string.
     * <p>Example: {@code 550e8400-e29b-41d4-a716-446655440000} {@code ->}
     * {@code 550e8400e29b41d4a716446655440000}.</p>
     *
     * @param uuid the string, typically 36 characters with hyphens; must not be {@code null}
     * @return the same characters without {@code '-'}
     */
    public static String compactUuid(String uuid) {
        return uuid.replace("-", "");
    }

    /**
     * Inserts hyphens into a 32-character compact hex string, producing the canonical UUID text form.
     * <p>If {@code compUuid} is not exactly 32 characters long, it is returned unchanged (including
     * {@code null}).</p>
     *
     * @param compUuid 32 hex digits without hyphens, or any other string
     * @return canonical 8-4-4-4-12 string, or {@code compUuid} when length is not 32
     */
    public static String expandUuid(String compUuid) {
        if (null == compUuid || compUuid.length() != 32) {
            return compUuid;
        }
        return hex2Uuid(compUuid).toString();
    }

    /**
     * Converts a supported value to {@link UUID} by runtime type.
     * <p>Delegates to {@link #convert2Uuid(UUID)}, {@link #convert2Uuid(byte[])}, or
     * {@link #convert2Uuid(String)}. {@link BigInteger} values are interpreted via
     * {@link #convertFromBigInteger(BigInteger)}. Any other type causes {@link IllegalArgumentException}.</p>
     *
     * @param uuidObj {@code null}, {@link UUID}, {@code byte[]}, {@link String}, or {@link BigInteger}
     * @return parsed UUID, or {@code null} when {@code uuidObj} is {@code null}
     * @throws IllegalArgumentException if {@code uuidObj} is neither {@code null} nor a supported type
     */
    public static UUID convert2Uuid(Object uuidObj) {
        if (uuidObj == null) {
            return null;
        }
        if (uuidObj instanceof UUID uuid) {
            return convert2Uuid(uuid);
        }
        if (uuidObj instanceof byte[] bytes) {
            return convert2Uuid(bytes);
        }
        if (uuidObj instanceof String uuidString) {
            return convert2Uuid(uuidString);
        }
        if (uuidObj instanceof BigInteger bigInteger) {
            return convertFromBigInteger(bigInteger);
        }
        throw new IllegalArgumentException("Unexpected UUID type: " + uuidObj.getClass());
    }

    /**
     * Pass-through for an existing {@link UUID}.
     *
     * @param uuid the UUID, or {@code null}
     * @return {@code uuid}
     */
    public static UUID convert2Uuid(UUID uuid) {
        return uuid;
    }

    /**
     * Parses a UUID from a byte array in one of several encodings.
     * <ul>
     *   <li>length {@code 0}: {@code null}</li>
     *   <li>length {@code 16}: RFC binary (MSB then LSB, big-endian)</li>
     *   <li>length {@code 32}: ASCII hex digits (same as {@link #hex2Uuid(byte[])})</li>
     *   <li>length {@code 36}: ASCII canonical form with hyphens</li>
     * </ul>
     *
     * @param bytes {@code null}, empty, or 16/32/36 bytes as above
     * @return the UUID, or {@code null} for {@code null} or empty array
     * @throws IllegalArgumentException if length is not 0, 16, 32, or 36
     */
    public static UUID convert2Uuid(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return switch (bytes.length) {
            case 0 -> null;
            case 16 -> binary2Uuid(bytes);
            case 32 -> hex2Uuid(bytes);
            case 36 -> UUID.fromString(new String(bytes, StandardCharsets.US_ASCII));
            default ->
                    throw new IllegalArgumentException("Invalid UUID byte length: " + bytes.length + ". Expected 16, 32 or 36 bytes.");
        };
    }

    /**
     * Parses a UUID from a string: trims, skips optional leading/trailing brace or square bracket characters
     * independently, and parses 16 bytes using PostgreSQL-compatible hyphen placement.
     * <p>Blank input after normalization yields {@code null}.</p>
     *
     * @param uuidString {@code null}, blank, compact/canonical string, brace/square-bracket wrapped form,
     *                   or variants with hyphens after groups of 4 hexadecimal digits
     * @return the UUID, or {@code null} for {@code null} or blank normalized string
     * @throws IllegalArgumentException if the normalized string cannot be interpreted as a UUID
     */
    public static UUID convert2Uuid(String uuidString) {
        if (uuidString == null) {
            return null;
        }
        int start = trimStart(uuidString, 0, uuidString.length());
        int end = trimEnd(uuidString, start, uuidString.length());
        if (start == end) {
            return null;
        }

        if (isOpeningWrapper(uuidString.charAt(start))) {
            start++;
        }
        if (start == end) {
            return null;
        }
        if (isClosingWrapper(uuidString.charAt(end - 1))) {
            end--;
        }
        if (start == end) {
            return null;
        }

        return parseUuidString(uuidString, start, end);
    }

    /**
     * Parses a UUID from 32 bytes that are ASCII hex digits (0-9, a-f, A-F), inserts hyphens,
     * and parses with {@link UUID#fromString(String)}.
     *
     * @param hexBytes exactly 32 ASCII hex bytes, or {@code null}
     * @return the UUID, or {@code null} if {@code hexBytes} is {@code null}
     * @throws IllegalArgumentException if length is not 32
     */
    public static UUID hex2Uuid(byte[] hexBytes) {
        if (null == hexBytes) {
            return null;
        }
        if (hexBytes.length != 32) {
            throw new IllegalArgumentException(Arrays.toString(hexBytes) + " is not a valid hex string");
        }

        long mostSigBits = 0;
        long leastSigBits = 0;
        for (int i = 0; i < 32; i++) {
            int value = hexValue((char) hexBytes[i]);
            if (value < 0) {
                throw new IllegalArgumentException(Arrays.toString(hexBytes) + " is not a valid hex string");
            }
            if (i < 16) {
                mostSigBits = (mostSigBits << 4) | value;
            } else {
                leastSigBits = (leastSigBits << 4) | value;
            }
        }
        return new UUID(mostSigBits, leastSigBits);
    }

    /**
     * Parses a UUID from a 32-character hex string (no hyphens), building the canonical form
     * in a {@code char} buffer and parsing with {@link UUID#fromString(String)}.
     *
     * @param hexString exactly 32 hex digits, or {@code null}
     * @return the UUID, or {@code null} if {@code hexString} is {@code null}
     * @throws IllegalArgumentException if length is not 32
     */
    public static UUID hex2Uuid(String hexString) {
        if (null == hexString) {
            return null;
        }
        if (hexString.length() != 32) {
            throw new IllegalArgumentException(hexString + " is not a valid hex string");
        }

        return parseUuidString(hexString, 0, hexString.length());
    }

    /**
     * Based on PostgreSQL:
     * <a href="https://github.com/postgres/postgres/blob/901ed9b352b41f034e17bc540725082a488fce31/src/backend/utils/adt/uuid.c#L131">string_to_uuid</a>
     */
    private static UUID parseUuidString(String uuidString, int start, int end) {
        long mostSigBits = 0;
        long leastSigBits = 0;

        int pos = start;
        for (int i = 0; i < 16; i++) {
            if (pos + 1 >= end) {
                throw new IllegalArgumentException("Invalid UUID string: " + uuidString);
            }

            int hi = hexValue(uuidString.charAt(pos));
            int lo = hexValue(uuidString.charAt(pos + 1));
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("Invalid UUID string: " + uuidString);
            }
            int value = (hi << 4) | lo;

            if (i < 8) {
                mostSigBits = (mostSigBits << 8) | value;
            } else {
                leastSigBits = (leastSigBits << 8) | value;
            }
            pos += 2;

            if (pos < end && uuidString.charAt(pos) == '-' && (i % 2) == 1 && i < 15) {
                pos++;
            }
        }

        if (pos != end) {
            throw new IllegalArgumentException("Invalid UUID string: " + uuidString);
        }
        return new UUID(mostSigBits, leastSigBits);
    }

    private static int trimStart(String s, int start, int end) {
        while (start < end && Character.isWhitespace(s.charAt(start))) {
            start++;
        }
        return start;
    }

    private static int trimEnd(String s, int start, int end) {
        while (start < end && Character.isWhitespace(s.charAt(end - 1))) {
            end--;
        }
        return end;
    }

    private static boolean isOpeningWrapper(char ch) {
        return ch == '[' || ch == '{';
    }

    private static boolean isClosingWrapper(char ch) {
        return ch == ']' || ch == '}';
    }

    private static int hexValue(char ch) {
        return ch < HEX_VALUES.length ? HEX_VALUES[ch] : -1;
    }

    /**
     * Encodes a UUID as 16 bytes: most significant bits first, then least significant bits,
     * each {@code long} in big-endian order (same layout as {@link java.nio.ByteBuffer} putLong).
     *
     * @param uuid the UUID, or {@code null}
     * @return a new 16-byte array, or {@code null} if {@code uuid} is {@code null}
     */
    public static byte[] uuid2binary(UUID uuid) {
        return uuid2binary(uuid, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Encodes a UUID as 16 bytes using the requested byte order for each 64-bit half.
     *
     * @param uuid the UUID, or {@code null}
     * @param byteOrder byte order for the most/least significant {@code long} halves; must not be {@code null}
     * @return a new 16-byte array, or {@code null} if {@code uuid} is {@code null}
     */
    public static byte[] uuid2binary(UUID uuid, ByteOrder byteOrder) {
        if (null == uuid) {
            return null;
        }
        if (null == byteOrder) {
            throw new IllegalArgumentException("Byte order must not be null");
        }
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.order(byteOrder);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    /**
     * Parses a UUID from 16 bytes: first 8 bytes are the most significant bits, last 8 the least
     * significant bits, both big-endian.
     *
     * @param bytes at least 16 bytes (only the first 16 are read), or {@code null}
     * @return the UUID, or {@code null} if {@code bytes} is {@code null}
     * @throws java.nio.BufferUnderflowException if fewer than 16 bytes are available
     */
    public static UUID binary2Uuid(byte[] bytes) {
        return binary2Uuid(bytes, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Parses a UUID from 16 bytes using the requested byte order for each 64-bit half.
     * <p>The default UUID/RFC/PostgreSQL binary representation is {@link ByteOrder#BIG_ENDIAN}; use
     * {@link ByteOrder#LITTLE_ENDIAN} only when the source explicitly stores the two 64-bit halves that way.</p>
     *
     * @param bytes at least 16 bytes (only the first 16 are read), or {@code null}
     * @param byteOrder byte order for the most/least significant {@code long} halves; must not be {@code null}
     * @return the UUID, or {@code null} if {@code bytes} is {@code null}
     * @throws java.nio.BufferUnderflowException if fewer than 16 bytes are available
     */
    public static UUID binary2Uuid(byte[] bytes, ByteOrder byteOrder) {
        if (null == bytes) {
            return null;
        }
        if (null == byteOrder) {
            throw new IllegalArgumentException("Byte order must not be null");
        }
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        bb.order(byteOrder);
        long mostSigBits = bb.getLong();
        long leastSigBits = bb.getLong();
        return new UUID(mostSigBits, leastSigBits);
    }

    /**
     * Encodes a UUID using Microsoft GUID binary layout.
     * <p>Unlike {@link ByteOrder#LITTLE_ENDIAN} for two {@code long} halves, GUID binary layout stores
     * the first three UUID fields ({@code 4-2-2} bytes) little-endian and keeps the remaining 8 bytes unchanged.</p>
     *
     * @param uuid the UUID, or {@code null}
     * @return a new 16-byte array in Microsoft GUID binary layout, or {@code null} if {@code uuid} is {@code null}
     */
    public static byte[] uuid2MicrosoftGuidBinary(UUID uuid) {
        if (null == uuid) {
            return null;
        }

        byte[] bytes = new byte[16];
        long mostSigBits = uuid.getMostSignificantBits();
        long leastSigBits = uuid.getLeastSignificantBits();

        bytes[0] = (byte) (mostSigBits >>> 32);
        bytes[1] = (byte) (mostSigBits >>> 40);
        bytes[2] = (byte) (mostSigBits >>> 48);
        bytes[3] = (byte) (mostSigBits >>> 56);
        bytes[4] = (byte) (mostSigBits >>> 16);
        bytes[5] = (byte) (mostSigBits >>> 24);
        bytes[6] = (byte) mostSigBits;
        bytes[7] = (byte) (mostSigBits >>> 8);
        bytes[8] = (byte) (leastSigBits >>> 56);
        bytes[9] = (byte) (leastSigBits >>> 48);
        bytes[10] = (byte) (leastSigBits >>> 40);
        bytes[11] = (byte) (leastSigBits >>> 32);
        bytes[12] = (byte) (leastSigBits >>> 24);
        bytes[13] = (byte) (leastSigBits >>> 16);
        bytes[14] = (byte) (leastSigBits >>> 8);
        bytes[15] = (byte) leastSigBits;
        return bytes;
    }

    /**
     * Parses a UUID from Microsoft GUID binary layout.
     * <p>The first three UUID fields ({@code 4-2-2} bytes) are read little-endian; the remaining 8 bytes
     * are read in their stored order.</p>
     *
     * @param bytes at least 16 bytes (only the first 16 are read), or {@code null}
     * @return the UUID, or {@code null} if {@code bytes} is {@code null}
     * @throws BufferUnderflowException if fewer than 16 bytes are available
     */
    public static UUID microsoftGuidBinary2Uuid(byte[] bytes) {
        if (null == bytes) {
            return null;
        }
        if (bytes.length < 16) {
            throw new BufferUnderflowException();
        }

        long mostSigBits = (((long) bytes[3] & 0xff) << 56)
                | (((long) bytes[2] & 0xff) << 48)
                | (((long) bytes[1] & 0xff) << 40)
                | (((long) bytes[0] & 0xff) << 32)
                | (((long) bytes[5] & 0xff) << 24)
                | (((long) bytes[4] & 0xff) << 16)
                | (((long) bytes[7] & 0xff) << 8)
                | ((long) bytes[6] & 0xff);
        long leastSigBits = (((long) bytes[8] & 0xff) << 56)
                | (((long) bytes[9] & 0xff) << 48)
                | (((long) bytes[10] & 0xff) << 40)
                | (((long) bytes[11] & 0xff) << 32)
                | (((long) bytes[12] & 0xff) << 24)
                | (((long) bytes[13] & 0xff) << 16)
                | (((long) bytes[14] & 0xff) << 8)
                | ((long) bytes[15] & 0xff);
        return new UUID(mostSigBits, leastSigBits);
    }

    /**
     * Returns whether {@code uuid} is accepted by {@link UUID#fromString(String)}.
     *
     * @param uuid the string to test
     * @return {@code true} if parsing succeeds, {@code false} on {@link IllegalArgumentException} from {@code fromString}
     * @throws NullPointerException if {@code uuid} is {@code null}
     */
    public static boolean isUuid(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Maps a UUID to an unsigned 128-bit integer as {@link BigInteger}: high 64 bits in the high
     * half, low 64 bits in the low half, each half interpreted unsigned before combination.
     *
     * @param id the UUID, or {@code null}
     * @return {@code lo + hi * 2^64} with unsigned halves, or {@code null} if {@code id} is {@code null}
     */
    public static BigInteger convertToBigInteger(UUID id) {
        if (null == id) {
            return null;
        }
        BigInteger lo = BigInteger.valueOf(id.getLeastSignificantBits());
        BigInteger hi = BigInteger.valueOf(id.getMostSignificantBits());

        // If any of lo/hi parts is negative interpret as unsigned

        if (hi.signum() < 0) {
            hi = hi.add(B);
        }

        if (lo.signum() < 0) {
            lo = lo.add(B);
        }

        return lo.add(hi.multiply(B));
    }

    /**
     * Reconstructs a {@link UUID} from the unsigned 128-bit layout produced by {@link #convertToBigInteger(UUID)}.
     *
     * @param x the big integer, or {@code null}
     * @return the UUID, or {@code null} if {@code x} is {@code null}
     * @throws ArithmeticException if a half does not fit in a signed {@code long} after adjustment
     */
    public static UUID convertFromBigInteger(BigInteger x) {
        if (null == x) {
            return null;
        }
        BigInteger[] parts = x.divideAndRemainder(B);
        BigInteger hi = parts[0];
        BigInteger lo = parts[1];

        if (L.compareTo(lo) < 0) {
            lo = lo.subtract(B);
        }

        if (L.compareTo(hi) < 0) {
            hi = hi.subtract(B);
        }

        return new UUID(hi.longValueExact(), lo.longValueExact());
    }
}
