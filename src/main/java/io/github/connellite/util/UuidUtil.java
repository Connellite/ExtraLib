package io.github.connellite.util;

import lombok.experimental.UtilityClass;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

/**
 * Helpers for UUID strings, 16-byte binary form, and 32-char hex (with/without hyphens).
 */
@UtilityClass
public class UuidUtil {

    public static final BigInteger B = BigInteger.ONE.shiftLeft(64); // 2^64

    public static final BigInteger L = BigInteger.valueOf(Long.MAX_VALUE);

    /**
     * 550e8400-e29b-41d4-a716-446655440000 -> 550e8400e29b41d4a716446655440000
     */
    public static String compactUuid(String uuid) {
        return uuid.replace("-", "");
    }

    /**
     * 550e8400e29b41d4a716446655440000 -> 550e8400-e29b-41d4-a716-446655440000
     */
    public static String expandUuid(String compUuid) {
        if (null == compUuid || compUuid.length() != 32) {
            return compUuid;
        }
        return hex2Uuid(compUuid).toString();
    }

    public static UUID convert2Uuid(Object uuidObj) {
        if (uuidObj == null) {
            return null;
        }

        if (uuidObj instanceof UUID uuid) {
            return uuid;
        } else if (uuidObj instanceof byte[] bytes) {
            return switch (bytes.length) {
                case 0 -> null;
                case 16 -> UuidUtil.binary2Uuid(bytes);
                case 32 -> UuidUtil.hex2Uuid(bytes);
                case 36 -> UUID.fromString(new String(bytes, StandardCharsets.US_ASCII));
                default -> throw new IllegalArgumentException(
                        "Invalid UUID byte length: " + bytes.length + ". Expected 16, 32 or 36 bytes.");
            };
        } else if (uuidObj instanceof String uuidString) {
            return switch (uuidString.length()) {
                case 0 -> null;
                case 32 -> hex2Uuid(uuidString);
                case 36 -> UUID.fromString(uuidString);
                default -> throw new IllegalArgumentException("Invalid UUID string: " + uuidString);
            };
        } else {
            throw new IllegalArgumentException("Unexpected UUID type: " + uuidObj.getClass());
        }
    }

    /**
     * 32 ASCII hex bytes -> UUID
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
     * 32-char hex string -> UUID
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
     * 16-byte big-endian MSB/LSB -> UUID
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

    public static BigInteger convertToBigInteger(UUID id) {
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

    public static UUID convertFromBigInteger(BigInteger x) {
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
