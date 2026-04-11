package io.github.connellite.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
    void convertFromSquareString() {
        UUID u = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        assertEquals(u, UuidUtil.convert2Uuid("[550e8400-e29b-41d4-a716-446655440000]"));
        assertEquals(u, UuidUtil.convert2Uuid("[550e8400e29b41d4a716446655440000]"));
    }

    @Test
    void convertNullReturnsNull() {
        assertNull(UuidUtil.convert2Uuid((Object) null));
        assertNull(UuidUtil.convert2Uuid((UUID) null));
        assertNull(UuidUtil.convert2Uuid((String) null));
        assertNull(UuidUtil.convert2Uuid((byte[]) null));
    }
}
