package io.github.connellite.util;

import io.github.connellite.exception.TypeCoercionException;
import io.github.connellite.jdbc.LobUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.Period;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals("я", TypeCoercionUtil.coerce("я".toCharArray(), String.class));
        assertEquals("я", TypeCoercionUtil.coerce(NumberUtils.charsToObjectCharacters("я".toCharArray()), String.class));
        assertEquals("я", TypeCoercionUtil.coerce("я".getBytes(StandardCharsets.UTF_8), String.class));
        assertEquals("я", TypeCoercionUtil.coerce(NumberUtils.bytesToObjectBytes("я".getBytes(StandardCharsets.UTF_8)), String.class));
        assertEquals("я", TypeCoercionUtil.coerce(LobUtils.createBlob("я".getBytes(StandardCharsets.UTF_8)), String.class));
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
        assertEquals(uuid, TypeCoercionUtil.coerce(NumberUtils.bytesToObjectBytes(UuidUtil.uuid2binary(uuid)), UUID.class));
    }

    @Test
    void coerce_numericOverflowThrowsTypeCoercionException() {
        TypeCoercionException ex = assertThrows(
                TypeCoercionException.class,
                () -> TypeCoercionUtil.coerce(300, byte.class));
        assertInstanceOf(ArithmeticException.class, ex.getCause());
    }

    @Test
    void coerce_fractionalToIntegerThrowsTypeCoercionException() {
        TypeCoercionException ex = assertThrows(
                TypeCoercionException.class,
                () -> TypeCoercionUtil.coerce(1.5d, int.class));
        assertInstanceOf(ArithmeticException.class, ex.getCause());
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
        assertEquals(Color.RED, TypeCoercionUtil.coerce("red", Color.class));
        assertNull(TypeCoercionUtil.coerce("  ", Color.class));
    }

    @Test
    void coerce_booleanLiterals() {
        assertEquals(Boolean.TRUE, TypeCoercionUtil.coerce("yes", boolean.class));
        assertEquals(Boolean.TRUE, TypeCoercionUtil.coerce("on", Boolean.class));
        assertEquals(Boolean.FALSE, TypeCoercionUtil.coerce("N", boolean.class));
        assertEquals(Boolean.FALSE, TypeCoercionUtil.coerce("off", Boolean.class));
    }

    @Test
    void coerce_durationPeriodMonthDayOfWeekAndCalendar() {
        assertEquals(Duration.ofHours(1), TypeCoercionUtil.coerce("PT1H", Duration.class));
        assertEquals(Duration.ofSeconds(90), TypeCoercionUtil.coerce(90, Duration.class));
        assertEquals(Period.ofDays(1), TypeCoercionUtil.coerce("P1D", Period.class));
        assertEquals(Month.JANUARY, TypeCoercionUtil.coerce("january", Month.class));
        assertEquals(Month.MARCH, TypeCoercionUtil.coerce(3, Month.class));
        assertEquals(DayOfWeek.MONDAY, TypeCoercionUtil.coerce("monday", DayOfWeek.class));
        assertEquals(DayOfWeek.FRIDAY, TypeCoercionUtil.coerce(5, DayOfWeek.class));
        assertThrows(TypeCoercionException.class, () -> TypeCoercionUtil.coerce("nope", Duration.class));

        Calendar calendar = TypeCoercionUtil.coerce(1_700_000_000_000L, Calendar.class);
        assertEquals(1_700_000_000_000L, calendar.getTimeInMillis());
    }

    @Test
    void coerce_namedStringTypesAndNClob() throws Exception {
        assertEquals(Currency.getInstance("USD"), TypeCoercionUtil.coerce("usd", Currency.class));
        assertEquals(Locale.forLanguageTag("en-US"), TypeCoercionUtil.coerce("en_US", Locale.class));
        assertEquals(StandardCharsets.UTF_8, TypeCoercionUtil.coerce("UTF-8", Charset.class));
        assertEquals(TimeZone.getTimeZone("UTC"), TypeCoercionUtil.coerce("UTC", TimeZone.class));
        assertEquals(URI.create("https://example.com"), TypeCoercionUtil.coerce("https://example.com", URI.class));
        assertEquals(URI.create("https://example.com").toURL(), TypeCoercionUtil.coerce("https://example.com", java.net.URL.class));
        assertEquals(new File("a.txt"), TypeCoercionUtil.coerce("a.txt", File.class));
        assertEquals(Path.of("a.txt"), TypeCoercionUtil.coerce("a.txt", Path.class));
        assertThrows(TypeCoercionException.class, () -> TypeCoercionUtil.coerce("not-money", Currency.class));

        NClob nclob = TypeCoercionUtil.coerce("я", NClob.class);
        assertEquals("я", LobUtils.convertClobToString(nclob));
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
    }

    @Test
    void coerce_stringToByteAndCharArrays() {
        assertArrayEquals("text".getBytes(StandardCharsets.UTF_8), TypeCoercionUtil.coerce("text", byte[].class));
        assertArrayEquals("text".toCharArray(), TypeCoercionUtil.coerce("text", char[].class));
        assertArrayEquals("я".getBytes(StandardCharsets.UTF_8), TypeCoercionUtil.coerce("я", byte[].class));
        assertArrayEquals("я".toCharArray(), TypeCoercionUtil.coerce("я", char[].class));
        assertArrayEquals(new byte[0], TypeCoercionUtil.coerce("", byte[].class));
        assertArrayEquals(new char[0], TypeCoercionUtil.coerce("", char[].class));

        assertArrayEquals(
                "text".getBytes(StandardCharsets.UTF_8),
                NumberUtils.objectBytesToBytes(TypeCoercionUtil.coerce("text", Byte[].class)));
        assertArrayEquals(
                "text".toCharArray(),
                NumberUtils.objectCharactersToChars(TypeCoercionUtil.coerce("text", Character[].class)));
        assertNull(TypeCoercionUtil.coerce("text", int[].class));
    }

    @Test
    void coerce_arraySameInstance() {
        int[] ints = {1, 2};
        assertSame(ints, TypeCoercionUtil.coerce(ints, int[].class));
        byte[] bytes = {1, 2, 3};
        assertSame(bytes, TypeCoercionUtil.coerce(bytes, byte[].class));
    }

    @Test
    void coerce_primitiveWrapperArrayRoundTrip() {
        byte[] bytes = {0, 127, -1, -128};
        assertArrayEquals(bytes, TypeCoercionUtil.coerce(TypeCoercionUtil.coerce(bytes, Byte[].class), byte[].class));

        short[] shorts = {0, Short.MAX_VALUE, -1, Short.MIN_VALUE};
        assertArrayEquals(shorts, TypeCoercionUtil.coerce(TypeCoercionUtil.coerce(shorts, Short[].class), short[].class));

        int[] ints = {0, Integer.MAX_VALUE, -1, Integer.MIN_VALUE};
        assertArrayEquals(ints, TypeCoercionUtil.coerce(TypeCoercionUtil.coerce(ints, Integer[].class), int[].class));

        long[] longs = {0L, Long.MAX_VALUE, -1L, Long.MIN_VALUE};
        assertArrayEquals(longs, TypeCoercionUtil.coerce(TypeCoercionUtil.coerce(longs, Long[].class), long[].class));

        float[] floats = {0f, -1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY};
        assertArrayEquals(floats, TypeCoercionUtil.coerce(TypeCoercionUtil.coerce(floats, Float[].class), float[].class));

        double[] doubles = {0d, -1d, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        assertArrayEquals(doubles, TypeCoercionUtil.coerce(TypeCoercionUtil.coerce(doubles, Double[].class), double[].class));

        char[] chars = {'\0', 'A', Character.MAX_VALUE};
        assertArrayEquals(chars, TypeCoercionUtil.coerce(TypeCoercionUtil.coerce(chars, Character[].class), char[].class));

        boolean[] booleans = {true, false};
        assertArrayEquals(booleans, TypeCoercionUtil.coerce(TypeCoercionUtil.coerce(booleans, Boolean[].class), boolean[].class));

        assertArrayEquals(new Integer[0], TypeCoercionUtil.coerce(new int[0], Integer[].class));
    }

    @Test
    void coerce_arrayNullElementToPrimitiveThrows() {
        assertThrows(TypeCoercionException.class, () -> TypeCoercionUtil.coerce(new Byte[]{1, null}, byte[].class));
        assertThrows(TypeCoercionException.class, () -> TypeCoercionUtil.coerce(new Integer[]{1, null}, int[].class));
        assertThrows(TypeCoercionException.class, () -> TypeCoercionUtil.coerce(new Boolean[]{true, null}, boolean[].class));
    }

    @Test
    void coerce_stringArrayToIntArray() {
        assertArrayEquals(new int[]{1, 2}, TypeCoercionUtil.coerce(new String[]{"1", "2"}, int[].class));
    }

    @Test
    void coerce_wrapperArrayKeepsNullElement() {
        Integer[] source = {1, null};
        Integer[] coerced = TypeCoercionUtil.coerce(new Long[]{1L, null}, Integer[].class);
        assertArrayEquals(source, coerced);
    }

    @Test
    void coerce_clobAndBlobTargets() throws Exception {
        Clob clob = TypeCoercionUtil.coerce("abc", Clob.class);
        assertEquals("abc", LobUtils.convertClobToString(clob));

        byte[] bytes = {9, 8, 7};
        Blob blob = TypeCoercionUtil.coerce(bytes, Blob.class);
        assertArrayEquals(bytes, LobUtils.convertBlobToByteArray(blob));
        Blob fromBoxed = TypeCoercionUtil.coerce(NumberUtils.bytesToObjectBytes(bytes), Blob.class);
        assertArrayEquals(bytes, LobUtils.convertBlobToByteArray(fromBoxed));

        assertEquals("я", LobUtils.convertClobToString(TypeCoercionUtil.coerce("я".toCharArray(), Clob.class)));
        assertEquals("я", LobUtils.convertClobToString(
                TypeCoercionUtil.coerce(NumberUtils.charsToObjectCharacters("я".toCharArray()), Clob.class)));
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
    void coerce_sqlTemporalsFromJavaTimeCalendarAndEpoch() {
        LocalDate localDate = LocalDate.of(2024, 3, 15);
        LocalTime localTime = LocalTime.of(14, 30, 45);
        LocalDateTime ldt = LocalDateTime.of(localDate, localTime);
        long dateMillis = java.sql.Date.valueOf(localDate).getTime();
        long timeMillis = Time.valueOf(localTime).getTime();
        long tsMillis = Timestamp.valueOf(ldt).getTime();

        assertEquals(new java.sql.Date(dateMillis), TypeCoercionUtil.coerce(localDate, java.sql.Date.class));
        assertEquals(new Time(timeMillis), TypeCoercionUtil.coerce(localTime, Time.class));
        assertEquals(new Timestamp(tsMillis), TypeCoercionUtil.coerce(ldt, Timestamp.class));

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(tsMillis);
        assertEquals(new Timestamp(tsMillis), TypeCoercionUtil.coerce(calendar, Timestamp.class));
        assertEquals(new java.sql.Date(tsMillis), TypeCoercionUtil.coerce(tsMillis, java.sql.Date.class));

        Instant instant = Instant.ofEpochMilli(tsMillis);
        assertEquals(new Timestamp(tsMillis), TypeCoercionUtil.coerce(instant, Timestamp.class));
        assertEquals(new Timestamp(tsMillis), TypeCoercionUtil.coerce(instant.atOffset(ZoneOffset.UTC), Timestamp.class));
        assertEquals(new Timestamp(tsMillis), TypeCoercionUtil.coerce(instant.atZone(ZoneOffset.UTC), Timestamp.class));
        assertEquals(new Timestamp(tsMillis), TypeCoercionUtil.coerce(Long.toString(tsMillis), Timestamp.class));
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
    void coerce_yearYearMonthMonthDay() {
        LocalDate date = LocalDate.of(2024, 3, 21);
        Instant instant = date.atStartOfDay(ZoneOffset.UTC).toInstant();

        assertEquals(Year.of(2024), TypeCoercionUtil.coerce(date, Year.class));
        assertEquals(Year.of(2024), TypeCoercionUtil.coerce(date.atTime(10, 15), Year.class));
        assertEquals(Year.from(DateTimeUtil.toLocalDate(instant)), TypeCoercionUtil.coerce(instant, Year.class));
        assertEquals(Year.of(2024), TypeCoercionUtil.coerce(2024, Year.class));
        assertEquals(Year.of(2024), TypeCoercionUtil.coerce("2024", Year.class));
        assertEquals(YearMonth.of(2024, 3), TypeCoercionUtil.coerce(date, YearMonth.class));
        assertEquals(YearMonth.of(2024, 3), TypeCoercionUtil.coerce("2024-03", YearMonth.class));
        assertEquals(YearMonth.of(2024, 1), TypeCoercionUtil.coerce("2024", YearMonth.class));
        assertEquals(MonthDay.of(3, 21), TypeCoercionUtil.coerce(date, MonthDay.class));
        assertEquals(MonthDay.of(1, 1), TypeCoercionUtil.coerce("2024", MonthDay.class));
        assertNull(TypeCoercionUtil.coerce("  ", Year.class));
        assertThrows(TypeCoercionException.class, () -> TypeCoercionUtil.coerce("not-a-date", Year.class));
        assertThrows(TypeCoercionException.class, () -> TypeCoercionUtil.coerce(1.5d, Year.class));
    }

    @Test
    void coerce_offsetTimeZoneIdZoneOffset() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 3, 21, 10, 15, 30);
        OffsetDateTime offsetDateTime = dateTime.atOffset(ZoneOffset.ofHours(3));
        ZonedDateTime zonedDateTime = dateTime.atZone(ZoneId.of("Europe/Moscow"));
        OffsetTime offsetTime = OffsetTime.of(LocalTime.of(10, 15, 30), ZoneOffset.ofHours(3));

        assertEquals(offsetDateTime.toOffsetTime(), TypeCoercionUtil.coerce(offsetDateTime, OffsetTime.class));
        assertEquals(zonedDateTime.toOffsetDateTime().toOffsetTime(), TypeCoercionUtil.coerce(zonedDateTime, OffsetTime.class));
        assertEquals(offsetTime, TypeCoercionUtil.coerce("2024-03-21T10:15:30+03:00", OffsetTime.class));
        assertNull(TypeCoercionUtil.coerce("   ", OffsetTime.class));
        assertThrows(TypeCoercionException.class, () -> TypeCoercionUtil.coerce("10:15:30", OffsetTime.class));

        assertEquals(ZoneId.of("Europe/Moscow"), TypeCoercionUtil.coerce(zonedDateTime, ZoneId.class));
        assertEquals(ZoneId.of("Europe/Moscow"), TypeCoercionUtil.coerce(TimeZone.getTimeZone("Europe/Moscow"), ZoneId.class));
        assertEquals(ZoneOffset.ofHours(3), TypeCoercionUtil.coerce(offsetDateTime, ZoneId.class));
        assertEquals(ZoneId.of("Europe/Moscow"), TypeCoercionUtil.coerce("2024-03-21T10:15:30+03:00[Europe/Moscow]", ZoneId.class));
        assertEquals(ZoneId.of("Europe/Moscow"), TypeCoercionUtil.coerce("Europe/Moscow", ZoneId.class));
        assertEquals(ZoneOffset.ofHours(3), TypeCoercionUtil.coerce("2024-03-21T10:15:30+03:00", ZoneId.class));
        assertEquals(ZoneOffset.ofHours(3), TypeCoercionUtil.coerce("+03:00", ZoneId.class));
        assertEquals(ZoneOffset.ofHours(3), TypeCoercionUtil.coerce("+03:00", ZoneOffset.class));

        assertEquals(ZoneOffset.ofHours(3), TypeCoercionUtil.coerce(offsetDateTime, ZoneOffset.class));
        assertEquals(zonedDateTime.getOffset(), TypeCoercionUtil.coerce(zonedDateTime, ZoneOffset.class));
        assertEquals(ZoneOffset.ofHours(3), TypeCoercionUtil.coerce("2024-03-21T10:15:30+03:00", ZoneOffset.class));
        assertThrows(TypeCoercionException.class, () -> TypeCoercionUtil.coerce("2024-03-21T10:15:30[Europe/Moscow]", ZoneOffset.class));
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
