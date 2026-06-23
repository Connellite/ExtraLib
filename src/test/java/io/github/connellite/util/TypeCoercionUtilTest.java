package io.github.connellite.util;

import io.github.connellite.exception.TypeCoercionException;
import io.github.connellite.jdbc.LobUtils;
import org.junit.jupiter.api.Test;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeCoercionUtilTest {

    enum Color {
        RED, GREEN, BLUE
    }

    @Test
    void coerce_nullRawReturnsNull() {
        assertNull(TypeCoercionUtil.coerce(null, String.class));
        assertNull(TypeCoercionUtil.coerce(null, int.class));
    }

    @Test
    void coerce_nullTargetTypeThrows() {
        assertThrows(NullPointerException.class, () -> TypeCoercionUtil.coerce("x", null));
    }

    @Test
    void coerce_stringFromVariousSources() throws Exception {
        assertEquals("hello", TypeCoercionUtil.coerce("hello", String.class));
        assertEquals("42", TypeCoercionUtil.coerce(42, String.class));
        assertEquals("true", TypeCoercionUtil.coerce(true, String.class));

        Clob clob = LobUtils.createClob("clob-text");
        assertEquals("clob-text", TypeCoercionUtil.coerce(clob, String.class));

        assertEquals("[1, 2]", TypeCoercionUtil.coerce(new int[]{1, 2}, String.class));
    }

    @Test
    void coerce_numbers() {
        assertEquals(42, TypeCoercionUtil.coerce(42L, int.class));
        assertEquals(42L, TypeCoercionUtil.coerce(42, long.class));
        assertEquals((short) 7, TypeCoercionUtil.coerce("7", short.class));
        assertEquals(3.5d, TypeCoercionUtil.coerce("3.5", double.class));
    }

    @Test
    void coerce_boolean() {
        assertEquals(Boolean.TRUE, TypeCoercionUtil.coerce(1, boolean.class));
        assertEquals(Boolean.FALSE, TypeCoercionUtil.coerce(0L, Boolean.class));
        assertEquals(Boolean.TRUE, TypeCoercionUtil.coerce("true", boolean.class));
        assertNull(TypeCoercionUtil.coerce("maybe", Boolean.class));
    }

    @Test
    void coerce_character() {
        assertEquals('A', TypeCoercionUtil.coerce('A', char.class));
        assertEquals(Character.valueOf('Z'), TypeCoercionUtil.coerce(90, Character.class));
        assertEquals(Character.valueOf('x'), TypeCoercionUtil.coerce("x", char.class));
        assertNull(TypeCoercionUtil.coerce("", Character.class));
    }

    @Test
    void coerce_uuid() {
        UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        assertEquals(uuid, TypeCoercionUtil.coerce(uuid.toString(), UUID.class));
        assertEquals(uuid, TypeCoercionUtil.coerce(uuid, UUID.class));
    }

    @Test
    void coerce_numericOverflowThrowsTypeCoercionException() {
        TypeCoercionException ex = assertThrows(
                TypeCoercionException.class,
                () -> TypeCoercionUtil.coerce(300, byte.class));
        assertTrue(ex.getCause() instanceof ArithmeticException);
    }

    @Test
    void coerce_fractionalToIntegerThrowsTypeCoercionException() {
        TypeCoercionException ex = assertThrows(
                TypeCoercionException.class,
                () -> TypeCoercionUtil.coerce(1.5d, int.class));
        assertTrue(ex.getCause() instanceof ArithmeticException);
    }

    @Test
    void coerce_uuidInvalidThrows() {
        TypeCoercionException ex = assertThrows(
                TypeCoercionException.class,
                () -> TypeCoercionUtil.coerce("not-a-uuid", UUID.class));
        assertTrue(ex.getMessage().contains("UUID"));
    }

    @Test
    void coerce_enumByNameAndOrdinal() {
        assertEquals(Color.RED, TypeCoercionUtil.coerce("RED", Color.class));
        assertEquals(Color.GREEN, TypeCoercionUtil.coerce(" GREEN ", Color.class));
        assertEquals(Color.BLUE, TypeCoercionUtil.coerce(2, Color.class));
        assertNull(TypeCoercionUtil.coerce("  ", Color.class));
    }

    @Test
    void coerce_enumInvalidThrows() {
        assertThrows(TypeCoercionException.class, () -> TypeCoercionUtil.coerce("YELLOW", Color.class));
        assertThrows(TypeCoercionException.class, () -> TypeCoercionUtil.coerce(99, Color.class));
    }

    @Test
    void coerce_byteArrayAndBlob() throws Exception {
        byte[] bytes = {1, 2, 3};
        assertArrayEquals(bytes, TypeCoercionUtil.coerce(bytes, byte[].class));

        Blob blob = LobUtils.createBlob(bytes);
        assertArrayEquals(bytes, TypeCoercionUtil.coerce(blob, byte[].class));
        assertNull(TypeCoercionUtil.coerce("text", byte[].class));
    }

    @Test
    void coerce_clobAndBlobTargets() throws Exception {
        Clob clob = TypeCoercionUtil.coerce("abc", Clob.class);
        assertEquals("abc", LobUtils.convertClobToString(clob));

        byte[] bytes = {9, 8, 7};
        Blob blob = TypeCoercionUtil.coerce(bytes, Blob.class);
        assertArrayEquals(bytes, LobUtils.convertBlobToByteArray(blob));
    }

    @Test
    void coerce_sqlAndUtilDates() {
        LocalDate localDate = LocalDate.of(2024, 3, 15);
        java.sql.Date sqlDate = java.sql.Date.valueOf(localDate);
        Date utilDate = new Date(sqlDate.getTime());

        assertEquals(sqlDate, TypeCoercionUtil.coerce(utilDate, java.sql.Date.class));
        assertEquals(sqlDate, TypeCoercionUtil.coerce(localDate.toString(), java.sql.Date.class));
        assertEquals(utilDate, TypeCoercionUtil.coerce(sqlDate, Date.class));
    }

    @Test
    void coerce_sqlTimeAndTimestamp() {
        LocalDateTime ldt = LocalDateTime.of(2024, 6, 1, 14, 30, 45);
        Timestamp ts = Timestamp.valueOf(ldt);

        assertEquals(LocalTime.of(14, 30, 45), TypeCoercionUtil.coerce(ts, Time.class).toLocalTime());
        assertEquals(ts, TypeCoercionUtil.coerce(ldt.toString(), Timestamp.class));
    }

    @Test
    void coerce_javaTimeTypes() {
        LocalDate date = LocalDate.of(2024, 7, 10);
        LocalTime time = LocalTime.of(9, 15, 30);
        LocalDateTime dateTime = LocalDateTime.of(date, time);
        Instant instant = dateTime.toInstant(ZoneOffset.UTC);
        ZonedDateTime zoned = dateTime.atZone(ZoneOffset.ofHours(3));
        OffsetDateTime offset = zoned.toOffsetDateTime();

        assertEquals(date, TypeCoercionUtil.coerce(date.toString(), LocalDate.class));
        assertEquals(date, TypeCoercionUtil.coerce(Timestamp.valueOf(dateTime), LocalDate.class));
        assertEquals(dateTime, TypeCoercionUtil.coerce(Timestamp.valueOf(dateTime), LocalDateTime.class));
        assertEquals(time, TypeCoercionUtil.coerce(time.toString(), LocalTime.class));
        assertEquals(dateTime, TypeCoercionUtil.coerce(dateTime.toString(), LocalDateTime.class));
        assertEquals(Timestamp.valueOf(dateTime).toInstant(), TypeCoercionUtil.coerce(Timestamp.valueOf(dateTime), Instant.class));
        assertEquals(instant, TypeCoercionUtil.coerce(instant, Instant.class));
        assertEquals(offset, TypeCoercionUtil.coerce(offset, OffsetDateTime.class));
        assertEquals(zoned, TypeCoercionUtil.coerce(zoned, ZonedDateTime.class));
        assertEquals(offset, TypeCoercionUtil.coerce(zoned, OffsetDateTime.class));
    }

    @Test
    void coerce_javaTimeInvalidStringThrows() {
        TypeCoercionException ex = assertThrows(
                TypeCoercionException.class,
                () -> TypeCoercionUtil.coerce("not-a-date", LocalDate.class));
        assertTrue(ex.getMessage().contains("LocalDate"));
    }

    @Test
    void coerce_sameTypeInstance() {
        List<String> list = List.of("a");
        assertEquals(list, TypeCoercionUtil.coerce(list, List.class));
    }

    @Test
    void coerce_unsupportedTargetTypeThrows() {
        TypeCoercionException ex = assertThrows(
                TypeCoercionException.class,
                () -> TypeCoercionUtil.coerce("x", TypeCoercionUtilTest.class));
        assertTrue(ex.getMessage().contains("Unsupported target type"));
    }
}
