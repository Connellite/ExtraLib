package io.github.connellite.util;

import lombok.experimental.UtilityClass;

import java.math.BigInteger;
import java.nio.ByteBuffer;
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
     * {@link #convert2Uuid(String)}. Any other type causes {@link IllegalArgumentException}.</p>
     *
     * @param uuidObj {@code null}, {@link UUID}, {@code byte[]}, or {@link String}
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
     * Parses a UUID from a string: trims, strips optional {@code […]} brackets, then accepts
     * 32 hex digits (compact) or 36-character canonical form with hyphens.
     * <p>Blank input after normalization yields {@code null}.</p>
     *
     * @param uuidString {@code null}, blank, compact hex, canonical string, or bracketed variant
     * @return the UUID, or {@code null} for {@code null} or blank normalized string
     * @throws IllegalArgumentException if the normalized string length is not 0, 32, or 36
     */
    public static UUID convert2Uuid(String uuidString) {
        if (uuidString == null) {
            return null;
        }
        String normalized = normalizeUuidString(uuidString);
        return switch (normalized.length()) {
            case 0 -> null;
            case 32 -> hex2Uuid(normalized);
            case 36 -> UUID.fromString(normalized);
            default -> throw new IllegalArgumentException("Invalid UUID string: " + normalized);
        };
    }

    /**
     * Removes square brackets from a UUID string if present.
     * Example: "[550e8400-e29b-41d4-a716-446655440000]" -> "550e8400-e29b-41d4-a716-446655440000"
     *
     * @param uuidString the UUID string possibly wrapped in square brackets
     * @return the UUID string without brackets, or the original string if no brackets
     */
    private static String normalizeUuidString(String uuidString) {
        String s = uuidString.trim();
        if (s.length() >= 2 && s.charAt(0) == '[' && s.charAt(s.length() - 1) == ']') {
            return s.substring(1, s.length() - 1).trim();
        }
        return s;
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

        byte[] uuidBytes = new byte[36];
        System.arraycopy(hexBytes, 0, uuidBytes, 0, 8);
        uuidBytes[8] = '-';
        System.arraycopy(hexBytes, 8, uuidBytes, 9, 4);
        uuidBytes[13] = '-';
        System.arraycopy(hexBytes, 12, uuidBytes, 14, 4);
        uuidBytes[18] = '-';
        System.arraycopy(hexBytes, 16, uuidBytes, 19, 4);
        uuidBytes[23] = '-';
        System.arraycopy(hexBytes, 20, uuidBytes, 24, 12);
        return UUID.fromString(new String(uuidBytes, StandardCharsets.US_ASCII));
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

        char[] uuidBytes = new char[36];
        hexString.getChars(0, 8, uuidBytes, 0);
        uuidBytes[8] = '-';
        hexString.getChars(8, 12, uuidBytes, 9);
        uuidBytes[13] = '-';
        hexString.getChars(12, 16, uuidBytes, 14);
        uuidBytes[18] = '-';
        hexString.getChars(16, 20, uuidBytes, 19);
        uuidBytes[23] = '-';
        hexString.getChars(20, 32, uuidBytes, 24);
        return UUID.fromString(new String(uuidBytes));
    }

    /**
     * Encodes a UUID as 16 bytes: most significant bits first, then least significant bits,
     * each {@code long} in big-endian order (same layout as {@link java.nio.ByteBuffer} putLong).
     *
     * @param uuid the UUID, or {@code null}
     * @return a new 16-byte array, or {@code null} if {@code uuid} is {@code null}
     */
    public static byte[] uuid2binary(UUID uuid) {
        if (null == uuid) {
            return null;
        }
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
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
        if (null == bytes) {
            return null;
        }
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long mostSigBits = bb.getLong();
        long leastSigBits = bb.getLong();
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
