package io.github.connellite.util;

import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.List;

/**
 * Parses {@link LocalDate} and {@link LocalDateTime} from strings using a fixed set of patterns.
 * Throws if the input is blank or no pattern matches.
 */
@UtilityClass
public class DateTimeStringUtil {

    private static final List<DateTimeFormatter> INPUT_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ISO_DATE_TIME,
            new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                    .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
                    .toFormatter(),
            new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd HH:mm:ss")
                    .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
                    .toFormatter(),
            new DateTimeFormatterBuilder()
                    .appendPattern("yyyy/MM/dd HH:mm:ss")
                    .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
                    .toFormatter(),
            new DateTimeFormatterBuilder()
                    .appendPattern("dd.MM.yyyy HH:mm:ss")
                    .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
                    .toFormatter(),
            new DateTimeFormatterBuilder()
                    .appendPattern("dd/MM/yyyy HH:mm:ss")
                    .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
                    .toFormatter(),
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ISO_DATE,
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
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
}
