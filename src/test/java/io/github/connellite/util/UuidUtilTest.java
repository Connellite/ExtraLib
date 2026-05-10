package io.github.connellite.util;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UuidUtilTest {

    @Test
    void compactAndExpandRoundTrip() {
        String s = "550e8400-e29b-41d4-a716-446655440000";
        assertEquals("550e8400e29b41d4a716446655440000", UuidUtil.compactUuid(s));
        assertEquals(s, UuidUtil.expandUuid("550e8400e29b41d4a716446655440000"));
    }

    @Test
    void convertFromString() {
        UUID u = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        assertEquals(u, UuidUtil.convert2Uuid(u));
        assertEquals(u, UuidUtil.convert2Uuid("550e8400-e29b-41d4-a716-446655440000"));
        assertEquals(u, UuidUtil.convert2Uuid("550e8400e29b41d4a716446655440000"));
    }

    @Test
    void convertFromWrappedString() {
        UUID u = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        assertEquals(u, UuidUtil.convert2Uuid("{550e8400-e29b-41d4-a716-446655440000}"));
        assertEquals(u, UuidUtil.convert2Uuid("{550e8400e29b41d4a716446655440000}"));
        assertEquals(u, UuidUtil.convert2Uuid("[550e8400-e29b-41d4-a716-446655440000]"));
        assertEquals(u, UuidUtil.convert2Uuid("[550e8400e29b41d4a716446655440000]"));
    }

    @Test
    void convertFromSingleOrMixedWrapperString() {
        UUID u = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        assertEquals(u, UuidUtil.convert2Uuid("{550e8400-e29b-41d4-a716-446655440000"));
        assertEquals(u, UuidUtil.convert2Uuid("550e8400-e29b-41d4-a716-446655440000}"));
        assertEquals(u, UuidUtil.convert2Uuid("[550e8400-e29b-41d4-a716-446655440000"));
        assertEquals(u, UuidUtil.convert2Uuid("550e8400-e29b-41d4-a716-446655440000]"));
        assertEquals(u, UuidUtil.convert2Uuid("{550e8400-e29b-41d4-a716-446655440000]"));
        assertEquals(u, UuidUtil.convert2Uuid("    {550e8400-e29b-41d4-a716-446655440000]   "));
    }

    @Test
    void convertFromPostgresAndRfcCompatibleStrings() {
        UUID expected = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
        assertEquals(expected, UuidUtil.convert2Uuid("A0EEBC99-9C0B-4EF8-BB6D-6BB9BD380A11"));
        assertEquals(expected, UuidUtil.convert2Uuid("{a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11}"));
        assertEquals(expected, UuidUtil.convert2Uuid("a0eebc999c0b4ef8bb6d6bb9bd380a11"));
        assertEquals(expected, UuidUtil.convert2Uuid("a0ee-bc99-9c0b-4ef8-bb6d-6bb9-bd38-0a11"));
        assertEquals(expected, UuidUtil.convert2Uuid("{a0eebc99-9c0b4ef8-bb6d6bb9-bd380a11}"));
    }

    @Test
    void convertFromInvalidPostgresHyphenPositionThrows() {
        assertThrows(IllegalArgumentException.class, () -> UuidUtil.convert2Uuid("a0e-ebc999c0b4ef8bb6d6bb9bd380a11"));
        assertThrows(IllegalArgumentException.class, () -> UuidUtil.convert2Uuid("a0eebc99-9c0b-4ef8-bb6d-6bb9b-d380a11"));
    }

    @Test
    void convertFromObjectBigInteger() {
        UUID expected = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        BigInteger asBigInteger = UuidUtil.convertToBigInteger(expected);
        assertEquals(expected, UuidUtil.convert2Uuid((Object) asBigInteger));
    }

    @Test
    void uuidBinaryRoundTrip() {
        UUID expected = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
        byte[] binary = UuidUtil.uuid2binary(expected);
        assertEquals(expected, UuidUtil.convert2Uuid(binary));
        assertEquals(expected, UuidUtil.binary2Uuid(binary));
    }

    @Test
    void uuidBinaryRoundTripWithExplicitByteOrder() {
        UUID expected = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
        byte[] littleEndianBinary = UuidUtil.uuid2binary(expected, ByteOrder.LITTLE_ENDIAN);

        assertEquals(expected, UuidUtil.binary2Uuid(littleEndianBinary, ByteOrder.LITTLE_ENDIAN));
        assertNotEquals(expected, UuidUtil.binary2Uuid(littleEndianBinary, ByteOrder.BIG_ENDIAN));
    }

    @Test
    void microsoftGuidBinaryRoundTrip() {
        UUID expected = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
        byte[] microsoftGuidBytes = {
                (byte) 0x99, (byte) 0xbc, (byte) 0xee, (byte) 0xa0,
                0x0b, (byte) 0x9c,
                (byte) 0xf8, 0x4e,
                (byte) 0xbb, 0x6d, 0x6b, (byte) 0xb9, (byte) 0xbd, 0x38, 0x0a, 0x11
        };

        assertArrayEquals(microsoftGuidBytes, UuidUtil.uuid2MicrosoftGuidBinary(expected));
        assertEquals(expected, UuidUtil.microsoftGuidBinary2Uuid(microsoftGuidBytes));
        assertNotEquals(expected, UuidUtil.binary2Uuid(microsoftGuidBytes));
        assertNotEquals(expected, UuidUtil.binary2Uuid(microsoftGuidBytes, ByteOrder.LITTLE_ENDIAN));
    }

    @Test
    void parseFromHexBytesLength32() {
        UUID expected = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        byte[] hexBytes = "550e8400e29b41d4a716446655440000".getBytes(StandardCharsets.US_ASCII);
        assertEquals(expected, UuidUtil.convert2Uuid(hexBytes));
    }

    @Test
    void parseFromCanonicalBytesLength36() {
        UUID expected = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        byte[] canonicalBytes = "550e8400-e29b-41d4-a716-446655440000".getBytes(StandardCharsets.US_ASCII);
        assertEquals(expected, UuidUtil.convert2Uuid(canonicalBytes));
    }

    @Test
    void convertFromInvalidStringThrows() {
        assertThrows(IllegalArgumentException.class, () -> UuidUtil.convert2Uuid("{not-a-uuid}"));
    }

    @Test
    void convertFromEmptyAndBlankStringReturnsNull() {
        assertNull(UuidUtil.convert2Uuid(""));
        assertNull(UuidUtil.convert2Uuid(" "));
    }

    @Test
    void convertFromCompactStringInvalidLengthThrows() {
        assertThrows(IllegalArgumentException.class, () -> UuidUtil.convert2Uuid("550e8400e29b41d4a71644665544000"));
        assertThrows(IllegalArgumentException.class, () -> UuidUtil.convert2Uuid("550e8400e29b41d4a7164466554400000"));
    }

    @Test
    void convertFromStringWithInvalidCharactersThrows() {
        assertThrows(IllegalArgumentException.class, () -> UuidUtil.convert2Uuid("g50e8400-e29b-41d4-a716-446655440000"));
        assertThrows(IllegalArgumentException.class, () -> UuidUtil.convert2Uuid("@50e8400-e29b-41d4-a716-446655440000"));
    }

    @Test
    void convertNullReturnsNull() {
        assertNull(UuidUtil.convert2Uuid((Object) null));
        assertNull(UuidUtil.convert2Uuid((UUID) null));
        assertNull(UuidUtil.convert2Uuid((String) null));
        assertNull(UuidUtil.convert2Uuid((byte[]) null));
    }
}
