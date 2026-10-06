package io.github.connellite.util;

import io.github.connellite.util.internal.dateparse.CalendarNames;
import io.github.connellite.util.internal.CharBounds;
import io.github.connellite.util.internal.dateparse.DateTimePatterns;
import io.github.connellite.util.internal.dateparse.DateTimeScanner;
import io.github.connellite.util.internal.dateparse.ParsedFields;
import lombok.experimental.UtilityClass;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;

/**
 * Parses {@link LocalDate}, {@link LocalDateTime} and {@link LocalTime} from strings of unknown
 * layout, and converts between legacy {@link Date}/{@link Calendar} and {@link java.time} types.
 *
 * <p>Parsing is done by {@link DateTimeScanner} in a single pass over the input. ISO and RFC 3339
 * date-times, SQL timestamps, compact {@code yyyyMMdd} forms, numeric dates with two- or
 * four-digit years, localized month and weekday names, ordinal day suffixes, AM/PM markers,
 * offsets and zone abbreviations are all recognised. A date missing its day or its month and day,
 * such as {@code 2024-03} or {@code 2024}, resolves to the first such day.
 *
 * <p>A Unix epoch is read only by {@link #tryParseEpoch(String)} and
 * {@link #tryParseLocalDateTime(String, ZoneId)}, since it names an instant and so needs a zone.
 *
 * <p>In an all-numeric date, a value above 12 is taken as the day, so both {@code 25/12/2024} and
 * {@code 12/25/2024} mean Christmas. When both values could be a month, the order follows the
 * {@linkplain java.util.Locale.Category#FORMAT formatting locale}: {@code 03/04/2024} is the
 * fourth of March in the US and the third of April in the UK. Dot-separated dates are always
 * day-first, because no month-first locale writes {@code 12.02.1999}.
 *
 * <p>An offset or zone in the input is accepted but never shifts the result: the returned value
 * holds the date and time exactly as written.
 */
@UtilityClass
public class DateTimeUtil {

    private static final class SystemDefaultZoneHolder {
        private static final ZoneId INSTANCE = ZoneId.systemDefault();
    }

    /**
     * @return parsed date; time-of-day is discarded if present, or {@code null} if {@code text} is null/blank
     * @throws IllegalArgumentException if {@code text} cannot be parsed
     */
    public static LocalDate parseLocalDate(String text) {
        if (text == null || text.isBlank()) return null;

        LocalDate date = tryParseLocalDate(text);
        if (date == null) {
            throw new IllegalArgumentException("Unparseable date: '" + text.trim() + "'");
        }
        return date;
    }

    /**
     * @return parsed date-time; date-only strings use start of day (00:00), or {@code null} if {@code text} is null/blank
     * @throws IllegalArgumentException if {@code text} cannot be parsed
     */
    public static LocalDateTime parseLocalDateTime(String text) {
        if (text == null || text.isBlank()) return null;

        LocalDateTime dateTime = tryParseLocalDateTime(text);
        if (dateTime == null) {
            throw new IllegalArgumentException("Unparseable date-time: '" + text.trim() + "'");
        }
        return dateTime;
    }

    /**
     * @return parsed local time, or {@code null} if {@code text} is null/blank
     * @throws IllegalArgumentException if {@code text} cannot be parsed
     */
    public static LocalTime parseLocalTime(String text) {
        if (text == null || text.isBlank()) return null;

        LocalTime time = tryParseLocalTime(text);
        if (time == null) {
            throw new IllegalArgumentException("Unparseable time: '" + text.trim() + "'");
        }
        return time;
    }

    /**
     * Same as {@link #parseLocalDate(String)} without the exception on unparseable input.
     *
     * @return parsed date, or {@code null} if {@code text} is null/blank or unparseable
     */
    public static LocalDate tryParseLocalDate(String text) {
        ParsedFields fields = parseFields(text);
        return fields == null ? null : fields.toLocalDate();
    }

    /**
     * Same as {@link #parseLocalDateTime(String)} without the exception on unparseable input.
     *
     * @return parsed date-time, or {@code null} if {@code text} is null/blank or unparseable
     */
    public static LocalDateTime tryParseLocalDateTime(String text) {
        ParsedFields fields = parseFields(text);
        return fields == null ? null : fields.toLocalDateTime();
    }

    /**
     * Same as {@link #parseLocalTime(String)} without the exception on unparseable input.
     *
     * @return parsed time, or {@code null} if {@code text} is null/blank or unparseable
     */
    public static LocalTime tryParseLocalTime(String text) {
        ParsedFields fields = parseFields(text);
        return fields == null ? null : fields.toLocalTime();
    }

    /**
     * Reads an {@link OffsetTime} from {@code text} with the same recognition as
     * {@link #tryParseLocalTime(String)}.
     * The result is {@code null} when the input has no time, or has a time but neither an offset nor a zone.
     *
     * @return the offset time, or {@code null} if {@code text} is null/blank or does not carry one
     */
    public static OffsetTime tryParseOffsetTime(String text) {
        ParsedFields fields = parseFields(text);
        return fields == null ? null : fields.toOffsetTime();
    }

    /**
     * Reads a {@link ZoneId} from {@code text}.
     * A bare zone or offset is read by {@link ZoneId#of(String)}. Anything else, such as a date-time
     * that carries a zone, falls back to the scanner: a named zone wins, and an explicit offset is
     * used when the input named no zone.
     *
     * @return the zone, or {@code null} if {@code text} is null/blank or carries neither a zone nor an offset
     */
    public static ZoneId tryParseZoneId(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return ZoneId.of(text.trim());
        } catch (DateTimeException e) {
            ParsedFields fields = parseFields(text);
            return fields == null ? null : fields.toZoneId();
        }
    }

    /**
     * Reads a {@link ZoneOffset} from {@code text}.
     * A bare offset is read by {@link ZoneOffset#of(String)}. Anything else falls back to the scanner,
     * which returns only an offset written in the input. A named zone is left alone.
     *
     * @return the offset, or {@code null} if {@code text} is null/blank or carries no offset
     */
    public static ZoneOffset tryParseZoneOffset(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return ZoneOffset.of(text.trim());
        } catch (DateTimeException e) {
            ParsedFields fields = parseFields(text);
            return fields == null ? null : fields.toZoneOffset();
        }
    }

    /**
     * Same as {@link #tryParseLocalDateTime(String)}, additionally reading a Unix epoch and
     * resolving it in {@code zone}.
     *
     * <p>An epoch names an instant rather than a wall-clock reading, which is why it takes a zone
     * and why {@link #tryParseLocalDateTime(String)} leaves it alone: every other shape this class
     * parses is returned exactly as written, and no result depends on the zone of the machine.
     *
     * @param zone the zone to resolve an epoch in; must not be {@code null}
     * @return parsed date-time, or {@code null} if {@code text} is null/blank or unparseable
     */
    public static LocalDateTime tryParseLocalDateTime(String text, ZoneId zone) {
        Instant epoch = tryParseEpoch(text);
        return epoch == null ? tryParseLocalDateTime(text) : epoch.atZone(zone).toLocalDateTime();
    }

    /**
     * Reads a Unix epoch written as digits only, taking the unit from how many there are: ten
     * digits are seconds, thirteen milliseconds, sixteen microseconds and nineteen nanoseconds.
     *
     * <p>Eight and fourteen digits are not epochs: {@link #tryParseLocalDate(String)} reads them as
     * {@code yyyyMMdd} and {@code yyyyMMddHHmmss}, and no real epoch has either length.
     *
     * <p>Based on araddon/dateparse:
     * <a href="https://github.com/araddon/dateparse/blob/5dd51ed0f76a3790e35b502070d9acbb404fab30/parseany.go#L1734">dateDigit</a>,
     * which takes the unit from the length of the string in the same way. Lengths it leaves without
     * a result, and numbers too large for the unit, are rejected here rather than returning an
     * epoch of zero.
     *
     * @return the instant, or {@code null} if {@code text} is null/blank or is not such a number
     */
    public static Instant tryParseEpoch(String text) {
        if (text == null || text.isBlank()) return null;

        int start = CharBounds.trimStart(text, 0, text.length());
        int end = CharBounds.trimEnd(text, start, text.length());

        long value = 0;
        for (int i = start; i < end; i++) {
            int digit = text.charAt(i) - '0';
            if (digit < 0 || digit > 9 || value > (Long.MAX_VALUE - digit) / 10) return null;
            value = value * 10 + digit;
        }
        return switch (end - start) {
            case 10 -> Instant.ofEpochSecond(value);
            case 13 -> Instant.ofEpochMilli(value);
            case 16 -> Instant.ofEpochSecond(value / 1_000_000, value % 1_000_000 * 1_000);
            case 19 -> Instant.ofEpochSecond(value / 1_000_000_000, value % 1_000_000_000);
            default -> null;
        };
    }

    /**
     * Recognises {@code text} in up to three stages, each one only reached when the cheaper stage
     * before it found nothing: the scanner with the common name tables, the fallback formatters, and
     * finally the scanner with the name tables of every JDK language.
     *
     * @return the parsed fields, or {@code null} if {@code text} is null/blank or unparseable
     */
    private static ParsedFields parseFields(String text) {
        if (text == null || text.isBlank()) return null;

        int start = CharBounds.trimStart(text, 0, text.length());
        int end = CharBounds.trimEnd(text, start, text.length());

        ParsedFields fields = new ParsedFields();
        if (DateTimeScanner.scan(text, start, end, CalendarNames.primary(), fields)) {
            return fields;
        }
        fields.reset();
        if (DateTimePatterns.parse(text, start, end, fields)) {
            return fields;
        }
        fields.reset();
        if (DateTimeScanner.mayContainLocalizedNames(text, start, end)
                && DateTimeScanner.scan(text, start, end, CalendarNames.global(), fields)) {
            return fields;
        }
        return null;
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
