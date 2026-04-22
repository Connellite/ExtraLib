package io.github.connellite.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DateTimeUtilTest {

    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    @Test
    void parseLocalDateIso() {
        assertEquals(LocalDate.of(2011, 12, 3), DateTimeUtil.parseLocalDate("2011-12-03"));
    }

    @Test
    void parseLocalDateDayMonthString() {
        assertEquals(LocalDate.of(2024, 3, 21), DateTimeUtil.parseLocalDate("21 Mar 2024"));
    }

    @Test
    void parseLocalDateMonthDayString() {
        assertEquals(LocalDate.of(2024, 3, 21), DateTimeUtil.parseLocalDate("2024 Mar 21"));
    }

    @Test
    void parseLocalDateBasicIsoDate() {
        assertEquals(LocalDate.of(2024, 3, 21), DateTimeUtil.parseLocalDate("20240321"));
    }

    @Test
    void parseLocalDateRussianShortMonth() {
        assertEquals(LocalDate.of(2024, 3, 21), DateTimeUtil.parseLocalDate("21 мар. 2024"));
    }

    @Test
    void parseLocalDateRussianFullMonth() {
        assertEquals(LocalDate.of(2024, 3, 21), DateTimeUtil.parseLocalDate("21 марта 2024"));
    }

    @Test
    void parseLocalDateTimeWithFraction() {
        LocalDateTime dt = DateTimeUtil.parseLocalDateTime("2011-12-03T10:15:30.5");
        assertEquals(LocalDateTime.of(2011, 12, 3, 10, 15, 30, 500_000_000), dt);
    }

    @Test
    void parseLocalTimeFromTimeAndDateTime() {
        assertEquals(LocalTime.of(10, 15, 30), DateTimeUtil.parseLocalTime("10:15:30"));
        assertEquals(LocalTime.of(10, 15, 30), DateTimeUtil.parseLocalTime("2011-12-03T10:15:30"));
    }

    @Test
    void dateOnlyBecomesStartOfDayForDateTime() {
        assertEquals(
                LocalDate.of(1999, 2, 12).atStartOfDay(),
                DateTimeUtil.parseLocalDateTime("12.02.1999"));
    }

    @Test
    void unparseableThrows() {
        assertThrows(IllegalArgumentException.class, () -> DateTimeUtil.parseLocalDate("not-a-date"));
        assertNull(DateTimeUtil.parseLocalDateTime("  "));
        assertNull(DateTimeUtil.parseLocalTime(" "));
        assertThrows(IllegalArgumentException.class, () -> DateTimeUtil.parseLocalTime("not-a-time"));
    }

    @Test
    void startAndEndOfDayUseSameDate() {
        Date source = Date.from(LocalDateTime.of(2026, 3, 31, 14, 25, 30, 123_000_000)
                .atZone(SYSTEM_ZONE)
                .toInstant());

        LocalDateTime start = DateTimeUtil.startOfDay(source).toInstant().atZone(SYSTEM_ZONE).toLocalDateTime();
        LocalDateTime end = DateTimeUtil.endOfDay(source).toInstant().atZone(SYSTEM_ZONE).toLocalDateTime();

        assertEquals(LocalDateTime.of(2026, 3, 31, 0, 0, 0, 0), start);
        assertEquals(LocalDateTime.of(2026, 3, 31, 23, 59, 59, 999_000_000), end);
    }

    @Test
    void convertsUtilDateToSqlTypes() {
        Date source = Date.from(LocalDateTime.of(2026, 3, 31, 7, 8, 9, 456_000_000)
                .atZone(SYSTEM_ZONE)
                .toInstant());

        assertEquals(LocalDate.of(2026, 3, 31), DateTimeUtil.toSqlDate(source).toLocalDate());
        assertEquals(LocalTime.of(7, 8, 9), DateTimeUtil.toSqlTime(source).toLocalTime());
        assertEquals(source.toInstant(), DateTimeUtil.toSqlTimestamp(source).toInstant());
    }

    @Test
    void toDateFromLocalDateAndLocalDateTime() {
        LocalDate localDate = LocalDate.of(2026, 3, 31);
        assertEquals(
                Date.from(localDate.atStartOfDay(SYSTEM_ZONE).toInstant()),
                DateTimeUtil.toDate(localDate));

        LocalDateTime localDateTime = LocalDateTime.of(2026, 3, 31, 15, 30, 45, 123_000_000);
        assertEquals(
                Date.from(localDateTime.atZone(SYSTEM_ZONE).toInstant()),
                DateTimeUtil.toDate(localDateTime));

        assertNull(DateTimeUtil.toDate((LocalDate) null));
        assertNull(DateTimeUtil.toDate((LocalDateTime) null));
    }

    @Test
    void toDateFromInstantZonedOffsetAndCalendar() {
        Instant instant = Instant.parse("2026-04-11T08:15:30Z");
        assertEquals(Date.from(instant), DateTimeUtil.toDate(instant));

        ZonedDateTime zdt = ZonedDateTime.of(2026, 4, 11, 10, 0, 0, 0, ZoneId.of("Europe/Berlin"));
        assertEquals(Date.from(zdt.toInstant()), DateTimeUtil.toDate(zdt));

        OffsetDateTime odt = OffsetDateTime.of(2026, 4, 11, 3, 0, 0, 0, ZoneOffset.ofHours(-5));
        assertEquals(Date.from(odt.toInstant()), DateTimeUtil.toDate(odt));

        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.clear();
        cal.set(2026, Calendar.APRIL, 11, 8, 15, 30);
        assertEquals(Date.from(cal.toInstant()), DateTimeUtil.toDate(cal));

        assertNull(DateTimeUtil.toDate((Instant) null));
        assertNull(DateTimeUtil.toDate((ZonedDateTime) null));
        assertNull(DateTimeUtil.toDate((OffsetDateTime) null));
        assertNull(DateTimeUtil.toDate((Calendar) null));
    }

    @Test
    void toLocalDateTimeFromAllSupportedTypes() {
        Instant instant = Instant.parse("2026-04-11T08:15:30Z");
        assertEquals(instant.atZone(SYSTEM_ZONE).toLocalDateTime(), DateTimeUtil.toLocalDateTime(instant));

        LocalDateTime ldt = LocalDateTime.of(2026, 5, 20, 14, 30, 1, 999_000_000);
        assertEquals(ldt, DateTimeUtil.toLocalDateTime(ldt));

        LocalDate ld = LocalDate.of(2026, 7, 4);
        assertEquals(ld.atStartOfDay(SYSTEM_ZONE).toLocalDateTime(), DateTimeUtil.toLocalDateTime(ld));

        ZonedDateTime zdt = ZonedDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneId.of("UTC"));
        assertEquals(zdt.withZoneSameInstant(SYSTEM_ZONE).toLocalDateTime(), DateTimeUtil.toLocalDateTime(zdt));

        OffsetDateTime odt = OffsetDateTime.of(2026, 2, 28, 18, 0, 0, 0, ZoneOffset.ofHours(-8));
        assertEquals(odt.atZoneSameInstant(SYSTEM_ZONE).toLocalDateTime(), DateTimeUtil.toLocalDateTime(odt));

        Date date = Date.from(instant);
        assertEquals(instant.atZone(SYSTEM_ZONE).toLocalDateTime(), DateTimeUtil.toLocalDateTime(date));

        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.clear();
        cal.set(2026, Calendar.APRIL, 11, 8, 15, 30);
        assertEquals(cal.toInstant().atZone(SYSTEM_ZONE).toLocalDateTime(), DateTimeUtil.toLocalDateTime(cal));

        assertNull(DateTimeUtil.toLocalDateTime((LocalDateTime) null));
        assertNull(DateTimeUtil.toLocalDateTime((LocalDate) null));
        assertNull(DateTimeUtil.toLocalDateTime((Instant) null));
        assertNull(DateTimeUtil.toLocalDateTime((ZonedDateTime) null));
        assertNull(DateTimeUtil.toLocalDateTime((OffsetDateTime) null));
        assertNull(DateTimeUtil.toLocalDateTime((Date) null));
        assertNull(DateTimeUtil.toLocalDateTime((Calendar) null));
    }

    @Test
    void toLocalDateFromAllSupportedTypes() {
        Instant instant = Instant.parse("2026-04-11T08:15:30Z");
        assertEquals(instant.atZone(SYSTEM_ZONE).toLocalDate(), DateTimeUtil.toLocalDate(instant));

        LocalDate ld = LocalDate.of(2026, 8, 1);
        assertEquals(ld, DateTimeUtil.toLocalDate(ld));

        LocalDateTime ldt = LocalDateTime.of(2026, 5, 20, 14, 30, 1, 999_000_000);
        assertEquals(ldt.toLocalDate(), DateTimeUtil.toLocalDate(ldt));

        ZonedDateTime zdt = ZonedDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneId.of("UTC"));
        assertEquals(zdt.withZoneSameInstant(SYSTEM_ZONE).toLocalDate(), DateTimeUtil.toLocalDate(zdt));

        OffsetDateTime odt = OffsetDateTime.of(2026, 2, 28, 18, 0, 0, 0, ZoneOffset.ofHours(-8));
        assertEquals(odt.atZoneSameInstant(SYSTEM_ZONE).toLocalDate(), DateTimeUtil.toLocalDate(odt));

        Date date = Date.from(instant);
        assertEquals(instant.atZone(SYSTEM_ZONE).toLocalDate(), DateTimeUtil.toLocalDate(date));

        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.clear();
        cal.set(2026, Calendar.APRIL, 11, 8, 15, 30);
        assertEquals(cal.toInstant().atZone(SYSTEM_ZONE).toLocalDate(), DateTimeUtil.toLocalDate(cal));

        assertNull(DateTimeUtil.toLocalDate((LocalDate) null));
        assertNull(DateTimeUtil.toLocalDate((LocalDateTime) null));
        assertNull(DateTimeUtil.toLocalDate((Instant) null));
        assertNull(DateTimeUtil.toLocalDate((ZonedDateTime) null));
        assertNull(DateTimeUtil.toLocalDate((OffsetDateTime) null));
        assertNull(DateTimeUtil.toLocalDate((Date) null));
        assertNull(DateTimeUtil.toLocalDate((Calendar) null));
    }

    @Test
    void toOffsetDateTimeFromAllSupportedTypes() {
        OffsetDateTime odt = OffsetDateTime.of(2026, 2, 28, 18, 0, 0, 0, ZoneOffset.ofHours(-8));
        assertEquals(odt, DateTimeUtil.toOffsetDateTime(odt));

        ZonedDateTime zdt = ZonedDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneId.of("UTC"));
        assertEquals(zdt.toOffsetDateTime(), DateTimeUtil.toOffsetDateTime(zdt));

        ZoneId targetZone = ZoneId.of("Europe/Berlin");
        Instant instant = Instant.parse("2026-04-11T08:15:30Z");
        assertEquals(instant.atZone(targetZone).toOffsetDateTime(), DateTimeUtil.toOffsetDateTime(instant, targetZone));

        Date date = Date.from(instant);
        assertEquals(date.toInstant().atZone(targetZone).toOffsetDateTime(), DateTimeUtil.toOffsetDateTime(date, targetZone));

        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.clear();
        cal.set(2026, Calendar.APRIL, 11, 8, 15, 30);
        assertEquals(cal.toInstant().atZone(targetZone).toOffsetDateTime(), DateTimeUtil.toOffsetDateTime(cal, targetZone));

        assertNull(DateTimeUtil.toOffsetDateTime((OffsetDateTime) null));
        assertNull(DateTimeUtil.toOffsetDateTime((ZonedDateTime) null));
        assertNull(DateTimeUtil.toOffsetDateTime((Instant) null, targetZone));
        assertNull(DateTimeUtil.toOffsetDateTime((Date) null, targetZone));
        assertNull(DateTimeUtil.toOffsetDateTime((Calendar) null, targetZone));
    }

    @Test
    void toZonedDateTimeOverloadsWithoutZoneUseSystemDefault() {
        Instant instant = Instant.parse("2026-04-11T08:15:30Z");
        assertEquals(DateTimeUtil.toZonedDateTime(instant, SYSTEM_ZONE), DateTimeUtil.toZonedDateTime(instant));

        Date date = Date.from(instant);
        assertEquals(DateTimeUtil.toZonedDateTime(date, SYSTEM_ZONE), DateTimeUtil.toZonedDateTime(date));

        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.clear();
        cal.set(2026, Calendar.APRIL, 11, 8, 15, 30);
        assertEquals(DateTimeUtil.toZonedDateTime(cal, SYSTEM_ZONE), DateTimeUtil.toZonedDateTime(cal));

        LocalDateTime ldt = LocalDateTime.of(2026, 5, 20, 14, 30, 1, 999_000_000);
        assertEquals(DateTimeUtil.toZonedDateTime(ldt, SYSTEM_ZONE), DateTimeUtil.toZonedDateTime(ldt));

        LocalDate ld = LocalDate.of(2026, 8, 1);
        assertEquals(DateTimeUtil.toZonedDateTime(ld, SYSTEM_ZONE), DateTimeUtil.toZonedDateTime(ld));
    }

    @Test
    void toOffsetDateTimeOverloadsWithoutZoneUseSystemDefault() {
        Instant instant = Instant.parse("2026-04-11T08:15:30Z");
        assertEquals(DateTimeUtil.toOffsetDateTime(instant, SYSTEM_ZONE), DateTimeUtil.toOffsetDateTime(instant));

        Date date = Date.from(instant);
        assertEquals(DateTimeUtil.toOffsetDateTime(date, SYSTEM_ZONE), DateTimeUtil.toOffsetDateTime(date));

        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.clear();
        cal.set(2026, Calendar.APRIL, 11, 8, 15, 30);
        assertEquals(DateTimeUtil.toOffsetDateTime(cal, SYSTEM_ZONE), DateTimeUtil.toOffsetDateTime(cal));

        LocalDateTime ldt = LocalDateTime.of(2026, 5, 20, 14, 30, 1, 999_000_000);
        assertEquals(DateTimeUtil.toOffsetDateTime(ldt, SYSTEM_ZONE), DateTimeUtil.toOffsetDateTime(ldt));
    }

    @Test
    void toLocalTimeFromAllSupportedTypes() {
        LocalTime lt = LocalTime.of(9, 8, 7, 123_000_000);
        assertEquals(lt, DateTimeUtil.toLocalTime(lt));

        LocalDateTime ldt = LocalDateTime.of(2026, 5, 20, 14, 30, 1, 999_000_000);
        assertEquals(ldt.toLocalTime(), DateTimeUtil.toLocalTime(ldt));

        Instant instant = Instant.parse("2026-04-11T08:15:30Z");
        Date date = Date.from(instant);
        assertEquals(instant.atZone(SYSTEM_ZONE).toLocalTime(), DateTimeUtil.toLocalTime(date));

        assertNull(DateTimeUtil.toLocalTime((LocalTime) null));
        assertNull(DateTimeUtil.toLocalTime((LocalDateTime) null));
        assertNull(DateTimeUtil.toLocalTime((Date) null));
    }
}
