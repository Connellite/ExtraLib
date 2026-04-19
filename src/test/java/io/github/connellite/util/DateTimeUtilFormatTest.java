package io.github.connellite.util;

import io.github.connellite.exception.FormatException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateTimeUtilFormatTest {

    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    @Test
    void strftimePercentZMatchesPosixBasicOffset() {
        ZonedDateTime kolkata = ZonedDateTime.of(2026, 6, 15, 12, 0, 0, 0, ZoneId.of("Asia/Kolkata"));
        assertEquals("+0530", DateTimeUtilFormat.strftime(Locale.ROOT, kolkata, "%z"));

        ZonedDateTime ny = ZonedDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneId.of("America/New_York"));
        assertEquals("-0500", DateTimeUtilFormat.strftime(Locale.ROOT, ny, "%z"));

        ZonedDateTime utc = ZonedDateTime.of(2026, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        assertEquals("+0000", DateTimeUtilFormat.strftime(Locale.ROOT, utc, "%z"));

        ZonedDateTime quarterHour = ZonedDateTime.of(2026, 7, 1, 12, 0, 0, 0, ZoneId.of("UTC+12:45"));
        assertEquals("+1245", DateTimeUtilFormat.strftime(Locale.ROOT, quarterHour, "%z"));
    }

    @Test
    void strftimePercentZHasNoColonUnlikeOffsetId() {
        ZonedDateTime z = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.of("Asia/Kolkata"));
        String zPart = DateTimeUtilFormat.strftime(Locale.ROOT, z, "%z");
        assertFalse(zPart.contains(":"), "POSIX %z is basic ±hhmm without colon");
        assertTrue(zPart.matches("[+-]\\d{4}"));
    }

    @Test
    void strftimePercentZIsIndependentOfLocale() {
        ZonedDateTime z = ZonedDateTime.of(2026, 6, 15, 12, 0, 0, 0, ZoneId.of("Europe/Berlin"));
        String de = DateTimeUtilFormat.strftime(Locale.GERMAN, z, "%z");
        String jp = DateTimeUtilFormat.strftime(Locale.JAPAN, z, "%z");
        assertEquals(de, jp);
    }

    @Test
    void strftimePercentZNonAsciiLocale() {
        ZonedDateTime z = ZonedDateTime.of(2026, 6, 15, 12, 0, 0, 0, ZoneId.of("Asia/Tokyo"));
        assertEquals("+0900", DateTimeUtilFormat.strftime(Locale.forLanguageTag("ja-JP"), z, "%z"));
    }

    @Test
    void strftimePercentZAbbreviatedZone() {
        ZonedDateTime z = ZonedDateTime.of(2026, 7, 15, 12, 0, 0, 0, ZoneId.of("UTC"));
        String abbrev = DateTimeUtilFormat.strftime(Locale.ROOT, z, "%Z");
        assertFalse(abbrev.isEmpty());
        assertEquals("UTC", abbrev);
    }

    @Test
    void strftimeOverloadDelegatesToZonedDateTime() {
        Instant instant = Instant.parse("2026-04-11T08:15:30Z");
        ZonedDateTime zdt = instant.atZone(SYSTEM_ZONE);
        Locale loc = Locale.ROOT;
        assertEquals(
                DateTimeUtilFormat.strftime(loc, zdt, "%Y-%m-%d %z"),
                DateTimeUtilFormat.strftime(loc, instant, SYSTEM_ZONE, "%Y-%m-%d %z"));
        assertEquals(
                DateTimeUtilFormat.strftime(loc, zdt, "%F"),
                DateTimeUtilFormat.strftime(loc, Date.from(instant), "%F"));
    }

    @Test
    void strftimeNullTemporalReturnsLiteralNull() {
        assertEquals("null", DateTimeUtilFormat.strftime(Locale.ROOT, (ZonedDateTime) null, "%Y"));
        assertEquals("null", DateTimeUtilFormat.strftime(Locale.ROOT, (Instant) null, "%Y"));
        assertEquals("null", DateTimeUtilFormat.strftime(Locale.ROOT, (LocalDate) null, "%Y"));
    }

    @Test
    void strftimeObjectUnsupportedThrows() {
        assertThrows(FormatException.class, () -> DateTimeUtilFormat.strftime(Locale.ROOT, "text", "%Y"));
    }

    @Test
    void strftimeCommonSpecifiers() {
        ZonedDateTime z = ZonedDateTime.of(2024, 6, 15, 14, 30, 45, 0, ZoneOffset.ofHours(2));
        Locale loc = Locale.US;
        assertEquals("2024-06-15", DateTimeUtilFormat.strftime(loc, z, "%F"));
        assertEquals("06/15/24", DateTimeUtilFormat.strftime(loc, z, "%D"));
        assertEquals("15", DateTimeUtilFormat.strftime(loc, z, "%d"));
        assertEquals("02:30:45 PM", DateTimeUtilFormat.strftime(loc, z, "%r"));
        assertEquals("%", DateTimeUtilFormat.strftime(loc, z, "%%"));
    }

    @Test
    void strftimeObjectPreservesZoneForZonedAndOffset() {
        ZonedDateTime berlin = ZonedDateTime.of(2026, 6, 15, 12, 0, 0, 0, ZoneId.of("Europe/Berlin"));
        assertEquals(
                DateTimeUtilFormat.strftime(Locale.ROOT, berlin, "%z"),
                DateTimeUtilFormat.strftime(Locale.ROOT, (Object) berlin, "%z"));

        OffsetDateTime odt = OffsetDateTime.of(2026, 6, 15, 12, 0, 0, 0, ZoneOffset.ofHours(3));
        assertEquals("+0300", DateTimeUtilFormat.strftime(Locale.ROOT, (Object) odt, "%z"));
    }

    @Test
    void strftimeEpochSecondsTabAndSpacePaddedHours() {
        ZonedDateTime utc = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        assertEquals("0", DateTimeUtilFormat.strftime(Locale.ROOT, utc, "%s"));

        ZonedDateTime z = ZonedDateTime.of(2026, 3, 9, 9, 0, 0, 0, ZoneOffset.UTC);
        assertEquals("x\ty", DateTimeUtilFormat.strftime(Locale.ROOT, z, "x%ty"));
        assertEquals(" 9", DateTimeUtilFormat.strftime(Locale.ROOT, z, "%k"));
        assertEquals(" 9", DateTimeUtilFormat.strftime(Locale.ROOT, z, "%l"));

        ZonedDateTime noon = ZonedDateTime.of(2026, 3, 9, 12, 0, 0, 0, ZoneOffset.UTC);
        assertEquals("12", DateTimeUtilFormat.strftime(Locale.ROOT, noon, "%l"));
        assertEquals("12", DateTimeUtilFormat.strftime(Locale.ROOT, noon, "%k"));
    }

    @Test
    void strftimePercentCmatchesCStyleLayout() {
        ZonedDateTime z = ZonedDateTime.of(2024, 6, 5, 15, 30, 0, 0, ZoneOffset.UTC);
        Locale us = Locale.US;
        assertEquals(
                DateTimeUtilFormat.strftime(us, z, "%a %b %e %H:%M:%S %Y"),
                DateTimeUtilFormat.strftime(us, z, "%c"));
    }

    @Test
    void strftimePercentUBeforeFirstSundayIsWeek00() {
        ZonedDateTime z = ZonedDateTime.of(2024, 1, 3, 0, 0, 0, 0, ZoneOffset.UTC);
        assertEquals("00", DateTimeUtilFormat.strftime(Locale.ROOT, z, "%U"));
        ZonedDateTime firstSun = ZonedDateTime.of(2024, 1, 7, 0, 0, 0, 0, ZoneOffset.UTC);
        assertEquals("01", DateTimeUtilFormat.strftime(Locale.ROOT, firstSun, "%U"));
    }

    @Test
    void strftimePercentWBeforeFirstMondayIsWeek00() {
        ZonedDateTime sunday = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        assertEquals("00", DateTimeUtilFormat.strftime(Locale.ROOT, sunday, "%W"));
        ZonedDateTime firstMon = ZonedDateTime.of(2023, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC);
        assertEquals("01", DateTimeUtilFormat.strftime(Locale.ROOT, firstMon, "%W"));
    }
}
