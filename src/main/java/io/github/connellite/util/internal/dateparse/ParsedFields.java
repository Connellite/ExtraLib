package io.github.connellite.util.internal.dateparse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Mutable holder for the date-time fields collected by {@link DateTimeScanner} and
 * {@link DateTimePatterns}.
 *
 * <p>Ranges are validated by the setters, so the {@code to*} methods never throw. A zone or offset
 * is recorded when present but does not shift the wall-clock fields: {@link #toLocalDateTime()}
 * returns the date-time exactly as written.
 */
public final class ParsedFields {

    private static final int HAS_DATE = 1;
    private static final int HAS_TIME = 1 << 1;
    private static final int HAS_OFFSET = 1 << 2;
    private static final int HAS_ZONE = 1 << 3;

    private static final int[] MONTH_LENGTHS = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    private int flags;
    private int year;
    private int month;
    private int day;
    private int hour;
    private int minute;
    private int second;
    private int nano;
    private int offsetSeconds;
    private ZoneId zone;

    /**
     * Clears all fields so the instance can be reused for another input.
     */
    public void reset() {
        flags = 0;
        zone = null;
    }

    /**
     * Records the calendar date.
     *
     * @return {@code false} when the values do not form a real date, leaving the instance unchanged
     */
    public boolean setDate(int year, int month, int day) {
        if (month < 1 || month > 12 || day < 1 || day > lengthOfMonth(year, month)) {
            return false;
        }
        this.year = year;
        this.month = month;
        this.day = day;
        flags |= HAS_DATE;
        return true;
    }

    /**
     * Records the time of day. A {@code second} of 60 (leap second) is clamped to 59.
     *
     * <p>Based on Jackson:
     * <a href="https://github.com/FasterXML/jackson-databind/blob/de62c677b12c02967c9620a11c1c5208f701cfef/src/main/java/com/fasterxml/jackson/databind/util/ISO8601Utils.java#L168">ISO8601Utils.parse</a>
     * ({@code if (seconds > 59 && seconds < 63) seconds = 59}).
     *
     * @return {@code false} when the values are out of range, leaving the instance unchanged
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean setTime(int hour, int minute, int second, int nano) {
        if (hour < 0 || hour > 23
                || minute < 0 || minute > 59
                || second < 0 || second > 60
                || nano < 0 || nano > 999_999_999) {
            return false;
        }
        this.hour = hour;
        this.minute = minute;
        this.second = second == 60 ? 59 : second;
        this.nano = nano;
        flags |= HAS_TIME;
        return true;
    }

    /**
     * Records a fixed offset from UTC.
     *
     * @return {@code false} when {@code offsetSeconds} exceeds the {@link ZoneOffset} range
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean setOffsetSeconds(int offsetSeconds) {
        if (offsetSeconds < -18 * 3600 || offsetSeconds > 18 * 3600) {
            return false;
        }
        this.offsetSeconds = offsetSeconds;
        flags |= HAS_OFFSET;
        return true;
    }

    /**
     * Records a named zone, and its current offset when no explicit offset was parsed.
     */
    public void setZone(ZoneId zone) {
        this.zone = zone;
        flags |= HAS_ZONE;
    }

    public boolean hasDate() {
        return (flags & HAS_DATE) != 0;
    }

    public boolean hasTime() {
        return (flags & HAS_TIME) != 0;
    }

    public boolean hasOffset() {
        return (flags & HAS_OFFSET) != 0;
    }

    public boolean hasZone() {
        return (flags & HAS_ZONE) != 0;
    }

    /**
     * @return the calendar date, or {@code null} when the input carried no date
     */
    public LocalDate toLocalDate() {
        if (!hasDate()) {
            return null;
        }
        return LocalDate.of(year, month, day);
    }

    /**
     * @return the date-time, using start of day when the input carried no time, or {@code null}
     * when the input carried no date
     */
    public LocalDateTime toLocalDateTime() {
        if (!hasDate()) {
            return null;
        }
        if (!hasTime()) {
            return LocalDate.of(year, month, day).atStartOfDay();
        }
        return LocalDateTime.of(year, month, day, hour, minute, second, nano);
    }

    /**
     * @return the time of day, {@link LocalTime#MIDNIGHT} for a date without time, or {@code null}
     * when the input carried neither
     */
    public LocalTime toLocalTime() {
        if (hasTime()) {
            return LocalTime.of(hour, minute, second, nano);
        }
        return hasDate() ? LocalTime.MIDNIGHT : null;
    }

    /**
     * @return the date-time at the parsed offset, falling back to the parsed zone's offset and then
     * to UTC, or {@code null} when the input carried no date
     */
    public OffsetDateTime toOffsetDateTime() {
        LocalDateTime dateTime = toLocalDateTime();
        if (dateTime == null) {
            return null;
        }
        return OffsetDateTime.of(dateTime, resolveOffset(dateTime));
    }

    /**
     * @return the time of day at the parsed offset or zone, or {@code null} when the input carried
     * no time or neither an offset nor a zone
     */
    public OffsetTime toOffsetTime() {
        if (!hasTime()) {
            return null;
        }
        ZoneOffset offset = toZoneOffset();
        if (offset == null && hasZone()) {
            if (zone instanceof ZoneOffset zoneOffset) {
                offset = zoneOffset;
            } else if (hasDate()) {
                offset = zone.getRules().getOffset(toLocalDateTime());
            }
        }
        if (offset == null) {
            return null;
        }
        return OffsetTime.of(LocalTime.of(hour, minute, second, nano), offset);
    }

    /**
     * @return the explicit offset written in the input, or {@code null} when there was none.
     * A named zone is not turned into an offset.
     */
    public ZoneOffset toZoneOffset() {
        if (!hasOffset()) {
            return null;
        }
        return ZoneOffset.ofTotalSeconds(offsetSeconds);
    }

    /**
     * @return the named zone, or the explicit offset when the input named no zone,
     * or {@code null} when it carried neither
     */
    public ZoneId toZoneId() {
        if (hasZone()) {
            return zone;
        }
        return toZoneOffset();
    }

    private ZoneOffset resolveOffset(LocalDateTime dateTime) {
        if (hasOffset()) {
            return ZoneOffset.ofTotalSeconds(offsetSeconds);
        }
        if (hasZone()) {
            return zone.getRules().getOffset(dateTime);
        }
        return ZoneOffset.UTC;
    }

    /**
     * Resolves a two-digit year against a sliding window starting 80 years before {@code baseYear},
     * the same pivot {@code SimpleDateFormat} uses.
     *
     * <p>Based on sqlite-jdbc:
     * <a href="https://github.com/xerial/sqlite-jdbc/blob/8e999fe889463c1cd532c3e7bcbc64d107999eac/src/main/java/org/sqlite/date/FastDateParser.java#L425">FastDateParser.adjustYear</a>.
     */
    public static int expandTwoDigitYear(int twoDigitYear, int baseYear) {
        int windowStart = baseYear - 80;
        int century = windowStart / 100 * 100;
        int candidate = century + twoDigitYear;
        return twoDigitYear >= windowStart - century ? candidate : candidate + 100;
    }

    private static int lengthOfMonth(int year, int month) {
        if (month == 2 && isLeapYear(year)) {
            return 29;
        }
        return MONTH_LENGTHS[month - 1];
    }

    private static boolean isLeapYear(int year) {
        return (year & 3) == 0 && (year % 100 != 0 || year % 400 == 0);
    }
}
