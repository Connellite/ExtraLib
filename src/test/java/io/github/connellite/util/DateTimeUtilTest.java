package io.github.connellite.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

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
    void dateOnlyBecomesStartOfDayForDateTime() {
        assertEquals(
                LocalDate.of(1999, 2, 12).atStartOfDay(),
                DateTimeUtil.parseLocalDateTime("12.02.1999"));
    }

    @Test
    void unparseableThrows() {
        assertThrows(IllegalArgumentException.class, () -> DateTimeUtil.parseLocalDate("not-a-date"));
        assertNull(DateTimeUtil.parseLocalDateTime("  "));
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
}
