package io.github.connellite.util;

import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Parses {@link LocalDate} and {@link LocalDateTime} from strings using a fixed set of patterns,
 * and converts between legacy {@link Date}/{@link Calendar} and {@link java.time} types.
 */
@UtilityClass
public class DateTimeUtil {

    private static final class SystemDefaultZoneHolder {
        private static final ZoneId INSTANCE = ZoneId.systemDefault();
    }

    private static final List<DateTimeFormatter> INPUT_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.RFC_1123_DATE_TIME,

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
            DateTimeFormatter.BASIC_ISO_DATE,
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),

            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("dd MMM yyyy")
                    .toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("yyyy MMM dd")
                    .toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("dd MMM yyyy")
                    .toFormatter(new Locale("ru")),
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("dd MMMM yyyy")
                    .toFormatter(new Locale("ru"))
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

    /**
     * @return parsed local time
     * @throws IllegalArgumentException if {@code text} is non-blank and cannot be parsed as a time/date-time
     */
    public static LocalTime parseLocalTime(String text) {
        if (text == null || text.isBlank()) return null;
        text = text.trim();
        try {
            return LocalTime.parse(text);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return toLocalTime(parseLocalDateTime(text));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unparseable time: '" + text + "'", e);
        }
    }

    /**
     * Converts {@code localDate} to a {@link Date} at the start of that calendar day in the
     * {@linkplain ZoneId#systemDefault() system default} time zone.
     *
     * @param localDate the local date, or {@code null}
     * @return {@code java.util.Date} at start of day in the system default zone, or {@code null} if {@code localDate} is {@code null}
     */
    public static Date toDate(LocalDate localDate) {
        if (localDate == null) return null;
        return Date.from(localDate.atStartOfDay(SystemDefaultZoneHolder.INSTANCE).toInstant());
    }

    /**
     * Converts {@code localDateTime} to a {@link Date} by placing {@code localDateTime} in the
     * {@linkplain ZoneId#systemDefault() system default} time zone.
     *
     * @param localDateTime the local date-time without zone, or {@code null}
     * @return the corresponding {@code java.util.Date}, or {@code null} if {@code localDateTime} is {@code null}
     */
    public static Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return Date.from(localDateTime.atZone(SystemDefaultZoneHolder.INSTANCE).toInstant());
    }

    /**
     * Converts {@code instant} to a {@link Date} (same instant on the time-line).
     *
     * @param instant the instant, or {@code null}
     * @return the corresponding {@code java.util.Date}, or {@code null} if {@code instant} is {@code null}
     */
    public static Date toDate(Instant instant) {
        if (instant == null) return null;
        return Date.from(instant);
    }

    /**
     * Converts {@code zonedDateTime} to a {@link Date} (same instant on the time-line).
     *
     * @param zonedDateTime the zoned date-time, or {@code null}
     * @return the corresponding {@code java.util.Date}, or {@code null} if {@code zonedDateTime} is {@code null}
     */
    public static Date toDate(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) return null;
        return Date.from(zonedDateTime.toInstant());
    }

    /**
     * Converts {@code offsetDateTime} to a {@link Date} (same instant on the time-line).
     *
     * @param offsetDateTime the offset date-time, or {@code null}
     * @return the corresponding {@code java.util.Date}, or {@code null} if {@code offsetDateTime} is {@code null}
     */
    public static Date toDate(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) return null;
        return Date.from(offsetDateTime.toInstant());
    }

    /**
     * Converts {@code calendar} to a {@link Date} (same instant on the time-line).
     *
     * @param calendar the calendar, or {@code null}
     * @return the corresponding {@code java.util.Date}, or {@code null} if {@code calendar} is {@code null}
     */
    public static Date toDate(Calendar calendar) {
        if (calendar == null) return null;
        return Date.from(calendar.toInstant());
    }

    /**
     * Returns {@code localDateTime} unchanged (null-safe).
     *
     * @param localDateTime the local date-time, or {@code null}
     * @return {@code localDateTime}, or {@code null} if {@code localDateTime} is {@code null}
     */
    public static LocalDateTime toLocalDateTime(LocalDateTime localDateTime) {
        return localDateTime;
    }

    /**
     * Converts {@code localDate} to {@link LocalDateTime} at the start of that calendar day in the
     * {@linkplain ZoneId#systemDefault() system default} time zone.
     *
     * @param localDate the local date, or {@code null}
     * @return start of day in the system default zone as {@code LocalDateTime}, or {@code null} if {@code localDate} is {@code null}
     */
    public static LocalDateTime toLocalDateTime(LocalDate localDate) {
        if (localDate == null) return null;
        return localDate.atStartOfDay(SystemDefaultZoneHolder.INSTANCE).toLocalDateTime();
    }

    /**
     * Converts {@code instant} to {@link LocalDateTime} in the {@linkplain ZoneId#systemDefault() system default} time zone.
     *
     * @param instant the instant, or {@code null}
     * @return local date-time in the system default zone, or {@code null} if {@code instant} is {@code null}
     */
    public static LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) return null;
        return instant.atZone(SystemDefaultZoneHolder.INSTANCE).toLocalDateTime();
    }

    /**
     * Converts {@code zonedDateTime} to {@link LocalDateTime} in the {@linkplain ZoneId#systemDefault() system default} time zone
     * (same instant on the time-line).
     *
     * @param zonedDateTime the zoned date-time, or {@code null}
     * @return local date-time in the system default zone, or {@code null} if {@code zonedDateTime} is {@code null}
     */
    public static LocalDateTime toLocalDateTime(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) return null;
        return zonedDateTime.withZoneSameInstant(SystemDefaultZoneHolder.INSTANCE).toLocalDateTime();
    }

    /**
     * Converts {@code offsetDateTime} to {@link LocalDateTime} in the {@linkplain ZoneId#systemDefault() system default} time zone
     * (same instant on the time-line).
     *
     * @param offsetDateTime the offset date-time, or {@code null}
     * @return local date-time in the system default zone, or {@code null} if {@code offsetDateTime} is {@code null}
     */
    public static LocalDateTime toLocalDateTime(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) return null;
        return offsetDateTime.atZoneSameInstant(SystemDefaultZoneHolder.INSTANCE).toLocalDateTime();
    }

    /**
     * Converts {@code date} to {@link LocalDateTime} in the {@linkplain ZoneId#systemDefault() system default} time zone.
     *
     * @param date the legacy date, or {@code null}
     * @return local date-time in the system default zone, or {@code null} if {@code date} is {@code null}
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(SystemDefaultZoneHolder.INSTANCE).toLocalDateTime();
    }

    /**
     * Converts {@code calendar} to {@link LocalDateTime} in the {@linkplain ZoneId#systemDefault() system default} time zone.
     *
     * @param calendar the calendar, or {@code null}
     * @return local date-time in the system default zone, or {@code null} if {@code calendar} is {@code null}
     */
    public static LocalDateTime toLocalDateTime(Calendar calendar) {
        if (calendar == null) return null;
        return calendar.toInstant().atZone(SystemDefaultZoneHolder.INSTANCE).toLocalDateTime();
    }

    /**
     * Returns {@code localDate} unchanged (null-safe).
     *
     * @param localDate the local date, or {@code null}
     * @return {@code localDate}, or {@code null} if {@code localDate} is {@code null}
     */
    public static LocalDate toLocalDate(LocalDate localDate) {
        return localDate;
    }

    /**
     * Extracts the calendar date from {@code localDateTime} (date part only; no time zone).
     *
     * @param localDateTime the local date-time, or {@code null}
     * @return {@link LocalDate} part of {@code localDateTime}, or {@code null} if {@code localDateTime} is {@code null}
     */
    public static LocalDate toLocalDate(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return localDateTime.toLocalDate();
    }

    /**
     * Converts {@code instant} to {@link LocalDate} in the {@linkplain ZoneId#systemDefault() system default} time zone.
     *
     * @param instant the instant, or {@code null}
     * @return the local calendar date in the system default zone, or {@code null} if {@code instant} is {@code null}
     */
    public static LocalDate toLocalDate(Instant instant) {
        if (instant == null) return null;
        return instant.atZone(SystemDefaultZoneHolder.INSTANCE).toLocalDate();
    }

    /**
     * Converts {@code zonedDateTime} to {@link LocalDate} in the {@linkplain ZoneId#systemDefault() system default} time zone
     * (same instant on the time-line).
     *
     * @param zonedDateTime the zoned date-time, or {@code null}
     * @return the local calendar date in the system default zone, or {@code null} if {@code zonedDateTime} is {@code null}
     */
    public static LocalDate toLocalDate(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) return null;
        return zonedDateTime.withZoneSameInstant(SystemDefaultZoneHolder.INSTANCE).toLocalDate();
    }

    /**
     * Converts {@code offsetDateTime} to {@link LocalDate} in the {@linkplain ZoneId#systemDefault() system default} time zone
     * (same instant on the time-line).
     *
     * @param offsetDateTime the offset date-time, or {@code null}
     * @return the local calendar date in the system default zone, or {@code null} if {@code offsetDateTime} is {@code null}
     */
    public static LocalDate toLocalDate(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) return null;
        return offsetDateTime.atZoneSameInstant(SystemDefaultZoneHolder.INSTANCE).toLocalDate();
    }

    /**
     * Converts {@code date} to {@link LocalDate} in the {@linkplain ZoneId#systemDefault() system default} time zone.
     *
     * @param date the legacy date, or {@code null}
     * @return the local calendar date in the system default zone, or {@code null} if {@code date} is {@code null}
     */
    public static LocalDate toLocalDate(Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(SystemDefaultZoneHolder.INSTANCE).toLocalDate();
    }

    /**
     * Converts {@code calendar} to {@link LocalDate} in the {@linkplain ZoneId#systemDefault() system default} time zone.
     *
     * @param calendar the calendar, or {@code null}
     * @return the local calendar date in the system default zone, or {@code null} if {@code calendar} is {@code null}
     */
    public static LocalDate toLocalDate(Calendar calendar) {
        if (calendar == null) return null;
        return calendar.toInstant().atZone(SystemDefaultZoneHolder.INSTANCE).toLocalDate();
    }

    /**
     * Returns {@code localTime} unchanged (null-safe).
     *
     * @param localTime the local time, or {@code null}
     * @return {@code localTime}, or {@code null} if {@code localTime} is {@code null}
     */
    public static LocalTime toLocalTime(LocalTime localTime) {
        return localTime;
    }

    /**
     * Extracts the local time-of-day from {@code localDateTime}.
     *
     * @param localDateTime the local date-time, or {@code null}
     * @return {@link LocalTime} part of {@code localDateTime}, or {@code null} if {@code localDateTime} is {@code null}
     */
    public static LocalTime toLocalTime(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return localDateTime.toLocalTime();
    }

    /**
     * Converts {@code date} to {@link LocalTime} in the {@linkplain ZoneId#systemDefault() system default} time zone.
     *
     * @param date the legacy date, or {@code null}
     * @return local time-of-day in the system default zone, or {@code null} if {@code date} is {@code null}
     */
    public static LocalTime toLocalTime(Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(SystemDefaultZoneHolder.INSTANCE).toLocalTime();
    }

    /**
     * @return {@link Date} at start of the local calendar day of {@code date} in the system default time zone
     */
    public static Date startOfDay(Date date) {
        if (date == null) return null;

        LocalDate localDate = date.toInstant().atZone(SystemDefaultZoneHolder.INSTANCE).toLocalDate();
        return Date.from(localDate.atStartOfDay(SystemDefaultZoneHolder.INSTANCE).toInstant());
    }

    /**
     * @return {@link Date} at end of the local calendar day of {@code date} in the system default time zone (23:59:59.999)
     */
    public static Date endOfDay(Date date) {
        if (date == null) return null;

        LocalDate localDate = date.toInstant().atZone(SystemDefaultZoneHolder.INSTANCE).toLocalDate();
        LocalDateTime endOfDay = localDate.atTime(23, 59, 59, 999_000_000);
        return Date.from(endOfDay.atZone(SystemDefaultZoneHolder.INSTANCE).toInstant());
    }

    /**
     * @return {@link java.sql.Date} for the local date of {@code date} in the system default time zone
     */
    public static java.sql.Date toSqlDate(Date date) {
        if (date == null) return null;

        LocalDate localDate = date.toInstant().atZone(SystemDefaultZoneHolder.INSTANCE).toLocalDate();
        return java.sql.Date.valueOf(localDate);
    }

    /**
     * @return {@link java.sql.Time} for the local time-of-day of {@code date} in the system default time zone
     */
    public static java.sql.Time toSqlTime(Date date) {
        if (date == null) return null;

        LocalTime localTime = date.toInstant().atZone(SystemDefaultZoneHolder.INSTANCE).toLocalTime();
        return java.sql.Time.valueOf(localTime);
    }

    /**
     * @return {@link java.sql.Timestamp} with the same instant as {@code date}
     */
    public static java.sql.Timestamp toSqlTimestamp(Date date) {
        if (date == null) return null;

        return java.sql.Timestamp.from(date.toInstant());
    }

    /**
     * Returns {@code zdt} unchanged (identity conversion for API symmetry with other {@code toZonedDateTime} overloads).
     *
     * @param zdt the zoned date-time, or {@code null}
     * @return {@code zdt}, or {@code null} if {@code zdt} is {@code null}
     */
    public static ZonedDateTime toZonedDateTime(ZonedDateTime zdt) {
        return zdt;
    }

    /**
     * Converts {@code zdt} to the same instant expressed in {@code zone}.
     *
     * @param zdt  the zoned date-time, or {@code null}
     * @param zone the target zone; must not be {@code null} when {@code zdt} is non-null
     * @return {@code zdt} with zone replaced by {@code zone}, or {@code null} if {@code zdt} is {@code null}
     */
    public static ZonedDateTime toZonedDateTime(ZonedDateTime zdt, ZoneId zone) {
        if (zdt == null) return null;
        return zdt.withZoneSameInstant(zone);
    }

    /**
     * Converts {@code odt} to a {@link ZonedDateTime} using the same offset as {@code odt}
     * (see {@link OffsetDateTime#toZonedDateTime()}).
     *
     * @param odt the offset date-time, or {@code null}
     * @return the zoned date-time, or {@code null} if {@code odt} is {@code null}
     */
    public static ZonedDateTime toZonedDateTime(OffsetDateTime odt) {
        if (odt == null) return null;
        return odt.toZonedDateTime();
    }

    /**
     * Converts {@code odt} to a {@link ZonedDateTime} at the same instant in {@code zone}.
     *
     * @param odt  the offset date-time, or {@code null}
     * @param zone the target zone; must not be {@code null} when {@code odt} is non-null
     * @return zoned date-time in {@code zone}, or {@code null} if {@code odt} is {@code null}
     */
    public static ZonedDateTime toZonedDateTime(OffsetDateTime odt, ZoneId zone) {
        if (odt == null) return null;
        return odt.atZoneSameInstant(zone);
    }

    /**
     * Converts {@code instant} to a {@link ZonedDateTime} in the {@linkplain ZoneId#systemDefault() system default} time zone.
     *
     * @param instant the instant, or {@code null}
     * @return {@code instant} at system default zone, or {@code null} if {@code instant} is {@code null}
     */
    public static ZonedDateTime toZonedDateTime(Instant instant) {
        return toZonedDateTime(instant, SystemDefaultZoneHolder.INSTANCE);
    }

    /**
     * Converts {@code instant} to a {@link ZonedDateTime} in {@code zone}.
     *
     * @param instant the instant, or {@code null}
     * @param zone    the zone; must not be {@code null} when {@code instant} is non-null
     * @return {@code instant} at {@code zone}, or {@code null} if {@code instant} is {@code null}
     */
    public static ZonedDateTime toZonedDateTime(Instant instant, ZoneId zone) {
        if (instant == null) return null;
        return instant.atZone(zone);
    }

    /**
     * Converts {@code date} to a {@link ZonedDateTime} in the {@linkplain ZoneId#systemDefault() system default} time zone
     * (same instant on the time-line).
     *
     * @param date the legacy date, or {@code null}
     * @return zoned date-time in system default zone, or {@code null} if {@code date} is {@code null}
     */
    public static ZonedDateTime toZonedDateTime(Date date) {
        return toZonedDateTime(date, SystemDefaultZoneHolder.INSTANCE);
    }

    /**
     * Converts {@code date} to a {@link ZonedDateTime} in {@code zone} (same instant on the time-line).
     *
     * @param date the legacy date, or {@code null}
     * @param zone the zone; must not be {@code null} when {@code date} is non-null
     * @return zoned date-time in {@code zone}, or {@code null} if {@code date} is {@code null}
     */
    public static ZonedDateTime toZonedDateTime(Date date, ZoneId zone) {
        if (date == null) return null;
        return date.toInstant().atZone(zone);
    }

    /**
     * Converts {@code calendar} to a {@link ZonedDateTime} in the {@linkplain ZoneId#systemDefault() system default} time zone
     * (same instant on the time-line).
     *
     * @param calendar the calendar, or {@code null}
     * @return zoned date-time in system default zone, or {@code null} if {@code calendar} is {@code null}
     */
    public static ZonedDateTime toZonedDateTime(Calendar calendar) {
        return toZonedDateTime(calendar, SystemDefaultZoneHolder.INSTANCE);
    }

    /**
     * Converts {@code calendar} to a {@link ZonedDateTime} in {@code zone} (same instant on the time-line).
     *
     * @param calendar the calendar, or {@code null}
     * @param zone     the zone; must not be {@code null} when {@code calendar} is non-null
     * @return zoned date-time in {@code zone}, or {@code null} if {@code calendar} is {@code null}
     */
    public static ZonedDateTime toZonedDateTime(Calendar calendar, ZoneId zone) {
        if (calendar == null) return null;
        return calendar.toInstant().atZone(zone);
    }

    /**
     * Interprets {@code ldt} as local date-time in the {@linkplain ZoneId#systemDefault() system default} time zone
     * (no offset shift of the clock fields).
     *
     * @param ldt the local date-time, or {@code null}
     * @return {@code ldt} at system default zone, or {@code null} if {@code ldt} is {@code null}
     */
    public static ZonedDateTime toZonedDateTime(LocalDateTime ldt) {
        return toZonedDateTime(ldt, SystemDefaultZoneHolder.INSTANCE);
    }

    /**
     * Interprets {@code ldt} as local date-time in {@code zone} (no offset shift of the clock fields).
     *
     * @param ldt  the local date-time, or {@code null}
     * @param zone the zone; must not be {@code null} when {@code ldt} is non-null
     * @return {@code ldt} at {@code zone}, or {@code null} if {@code ldt} is {@code null}
     */
    public static ZonedDateTime toZonedDateTime(LocalDateTime ldt, ZoneId zone) {
        if (ldt == null) return null;
        return ldt.atZone(zone);
    }

    /**
     * Converts {@code localDate} to start of that calendar day in the {@linkplain ZoneId#systemDefault() system default} time zone.
     *
     * @param localDate the local date, or {@code null}
     * @return midnight at {@code localDate} in system default zone, or {@code null} if {@code localDate} is {@code null}
     */
    public static ZonedDateTime toZonedDateTime(LocalDate localDate) {
        return toZonedDateTime(localDate, SystemDefaultZoneHolder.INSTANCE);
    }

    /**
     * Converts {@code localDate} to start of that calendar day in {@code zone}.
     *
     * @param localDate the local date, or {@code null}
     * @param zone      the zone; must not be {@code null} when {@code localDate} is non-null
     * @return midnight at {@code localDate} in {@code zone}, or {@code null} if {@code localDate} is {@code null}
     */
    public static ZonedDateTime toZonedDateTime(LocalDate localDate, ZoneId zone) {
        if (localDate == null) return null;
        return localDate.atStartOfDay(zone);
    }

    /**
     * Returns {@code odt} unchanged (identity conversion for API symmetry with other {@code toOffsetDateTime} overloads).
     *
     * @param odt the offset date-time, or {@code null}
     * @return {@code odt}, or {@code null} if {@code odt} is {@code null}
     */
    public static OffsetDateTime toOffsetDateTime(OffsetDateTime odt) {
        return odt;
    }

    /**
     * Converts {@code zdt} to an {@link OffsetDateTime} preserving the same instant and effective offset of {@code zdt}.
     *
     * @param zdt the zoned date-time, or {@code null}
     * @return offset date-time at the same instant, or {@code null} if {@code zdt} is {@code null}
     */
    public static OffsetDateTime toOffsetDateTime(ZonedDateTime zdt) {
        if (zdt == null) return null;
        return zdt.toOffsetDateTime();
    }

    /**
     * Converts {@code instant} to an {@link OffsetDateTime} in the {@linkplain ZoneId#systemDefault() system default} time zone.
     *
     * @param instant the instant, or {@code null}
     * @return offset date-time for {@code instant} in system default zone, or {@code null} if {@code instant} is {@code null}
     */
    public static OffsetDateTime toOffsetDateTime(Instant instant) {
        return toOffsetDateTime(instant, SystemDefaultZoneHolder.INSTANCE);
    }

    /**
     * Interprets {@code ldt} as local date-time in the {@linkplain ZoneId#systemDefault() system default} time zone.
     *
     * @param ldt the local date-time, or {@code null}
     * @return offset date-time in system default zone, or {@code null} if {@code ldt} is {@code null}
     */
    public static OffsetDateTime toOffsetDateTime(LocalDateTime ldt) {
        return toOffsetDateTime(ldt, SystemDefaultZoneHolder.INSTANCE);
    }

    /**
     * Interprets {@code ldt} as local date-time in {@code zone}.
     *
     * @param ldt  the local date-time, or {@code null}
     * @param zone the zone; must not be {@code null} when {@code ldt} is non-null
     * @return offset date-time in {@code zone}, or {@code null} if {@code ldt} is {@code null}
     */
    public static OffsetDateTime toOffsetDateTime(LocalDateTime ldt, ZoneId zone) {
        if (ldt == null) return null;
        return ldt.atZone(zone).toOffsetDateTime();
    }

    /**
     * Converts {@code instant} to an {@link OffsetDateTime} in {@code zone}.
     *
     * @param instant the instant, or {@code null}
     * @param zone    the zone; must not be {@code null} when {@code instant} is non-null
     * @return offset date-time for {@code instant} in {@code zone}, or {@code null} if {@code instant} is {@code null}
     */
    public static OffsetDateTime toOffsetDateTime(Instant instant, ZoneId zone) {
        if (instant == null) return null;
        return instant.atZone(zone).toOffsetDateTime();
    }

    /**
     * Converts {@code date} to an {@link OffsetDateTime} in the {@linkplain ZoneId#systemDefault() system default} time zone
     * (same instant on the time-line).
     *
     * @param date the legacy date, or {@code null}
     * @return offset date-time in system default zone, or {@code null} if {@code date} is {@code null}
     */
    public static OffsetDateTime toOffsetDateTime(Date date) {
        return toOffsetDateTime(date, SystemDefaultZoneHolder.INSTANCE);
    }

    /**
     * Converts {@code date} to an {@link OffsetDateTime} in {@code zone} (same instant on the time-line).
     *
     * @param date the legacy date, or {@code null}
     * @param zone the zone; must not be {@code null} when {@code date} is non-null
     * @return offset date-time in {@code zone}, or {@code null} if {@code date} is {@code null}
     */
    public static OffsetDateTime toOffsetDateTime(Date date, ZoneId zone) {
        if (date == null) return null;
        return date.toInstant().atZone(zone).toOffsetDateTime();
    }

    /**
     * Converts {@code calendar} to an {@link OffsetDateTime} in the {@linkplain ZoneId#systemDefault() system default} time zone
     * (same instant on the time-line).
     *
     * @param calendar the calendar, or {@code null}
     * @return offset date-time in system default zone, or {@code null} if {@code calendar} is {@code null}
     */
    public static OffsetDateTime toOffsetDateTime(Calendar calendar) {
        return toOffsetDateTime(calendar, SystemDefaultZoneHolder.INSTANCE);
    }

    /**
     * Converts {@code calendar} to an {@link OffsetDateTime} in {@code zone} (same instant on the time-line).
     *
     * @param calendar the calendar, or {@code null}
     * @param zone     the zone; must not be {@code null} when {@code calendar} is non-null
     * @return offset date-time in {@code zone}, or {@code null} if {@code calendar} is {@code null}
     */
    public static OffsetDateTime toOffsetDateTime(Calendar calendar, ZoneId zone) {
        if (calendar == null) return null;
        return calendar.toInstant().atZone(zone).toOffsetDateTime();
    }
}
