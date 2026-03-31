package io.github.connellite.util;

import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Parses {@link LocalDate} and {@link LocalDateTime} from strings using a fixed set of patterns.
 * Throws if the input is blank or no pattern matches.
 */
@UtilityClass
public class DateTimeUtil {

    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    private static final List<DateTimeFormatter> INPUT_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ISO_DATE_TIME,
            new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                    .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                    .toFormatter(),
            new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd HH:mm:ss")
                    .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                    .toFormatter(),
            new DateTimeFormatterBuilder()
                    .appendPattern("yyyy/MM/dd HH:mm:ss")
                    .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                    .toFormatter(),
            new DateTimeFormatterBuilder()
                    .appendPattern("dd.MM.yyyy HH:mm:ss")
                    .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                    .toFormatter(),
            new DateTimeFormatterBuilder()
                    .appendPattern("dd/MM/yyyy HH:mm:ss")
                    .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                    .toFormatter(),
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ISO_DATE,
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("dd MMM yyyy")
                    .toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("yyyy MMM dd")
                    .toFormatter(Locale.ENGLISH),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    );

    /**
     * @return parsed date; time-of-day is discarded if present
     * @throws IllegalArgumentException if {@code text} is null/blank or no formatter matches
     */
    public static LocalDate parseLocalDate(String text) {
        if (text == null || text.isBlank()) return null;
        text = text.trim();

        try {
            return OffsetDateTime.parse(text, DateTimeFormatter.ISO_DATE_TIME).toLocalDate();
        } catch (DateTimeParseException ignored) {
        }
        for (DateTimeFormatter formatter : INPUT_FORMATTERS) {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(text, formatter);
                return dateTime.toLocalDate();
            } catch (DateTimeParseException ignored) {
                try {
                    return LocalDate.parse(text, formatter);
                } catch (DateTimeParseException ignored2) {
                }
            }
        }
        throw new IllegalArgumentException("Unparseable date: '" + text + "'");
    }

    /**
     * @return parsed date-time; date-only strings use start of day (00:00)
     * @throws IllegalArgumentException if {@code text} is null/blank or no formatter matches
     */
    public static LocalDateTime parseLocalDateTime(String text) {
        if (text == null || text.isBlank()) return null;
        text = text.trim();

        try {
            return OffsetDateTime.parse(text, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }
        for (DateTimeFormatter formatter : INPUT_FORMATTERS) {
            try {
                return LocalDateTime.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
                try {
                    LocalDate date = LocalDate.parse(text, formatter);
                    return date.atStartOfDay();
                } catch (DateTimeParseException ignored2) {
                }
            }
        }
        throw new IllegalArgumentException("Unparseable date-time: '" + text + "'");
    }

    public static Date startOfDay(Date date) {
        if (date == null) return null;

        LocalDate localDate = date.toInstant().atZone(SYSTEM_ZONE).toLocalDate();
        return Date.from(localDate.atStartOfDay(SYSTEM_ZONE).toInstant());
    }

    public static Date endOfDay(Date date) {
        if (date == null) return null;

        LocalDate localDate = date.toInstant().atZone(SYSTEM_ZONE).toLocalDate();
        LocalDateTime endOfDay = localDate.atTime(23, 59, 59, 999_000_000);
        return Date.from(endOfDay.atZone(SYSTEM_ZONE).toInstant());
    }

    public static java.sql.Date toSqlDate(Date date) {
        if (date == null) return null;

        LocalDate localDate = date.toInstant().atZone(SYSTEM_ZONE).toLocalDate();
        return java.sql.Date.valueOf(localDate);
    }

    public static java.sql.Time toSqlTime(Date date) {
        if (date == null) return null;

        LocalTime localTime = date.toInstant().atZone(SYSTEM_ZONE).toLocalTime();
        return java.sql.Time.valueOf(localTime);
    }

    public static java.sql.Timestamp toSqlTimestamp(Date date) {
        if (date == null) return null;

        return java.sql.Timestamp.from(date.toInstant());
    }
}
