package io.github.connellite.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DateTimeStringUtilTest {

    @Test
    void parseLocalDateIso() {
        assertEquals(LocalDate.of(2011, 12, 3), DateTimeStringUtil.parseLocalDate("2011-12-03"));
    }

    @Test
    void parseLocalDateTimeWithFraction() {
        LocalDateTime dt = DateTimeStringUtil.parseLocalDateTime("2011-12-03T10:15:30.5");
        assertEquals(LocalDateTime.of(2011, 12, 3, 10, 15, 30, 500_000_000), dt);
    }

    @Test
    void dateOnlyBecomesStartOfDayForDateTime() {
        assertEquals(
                LocalDate.of(1999, 2, 12).atStartOfDay(),
                DateTimeStringUtil.parseLocalDateTime("12.02.1999"));
    }

    @Test
    void unparseableThrows() {
        assertThrows(IllegalArgumentException.class, () -> DateTimeStringUtil.parseLocalDate("not-a-date"));
        assertNull(DateTimeStringUtil.parseLocalDateTime("  "));
    }
}
