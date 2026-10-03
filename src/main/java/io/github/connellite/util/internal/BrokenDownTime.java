package io.github.connellite.util.internal;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;

/**
 * The fields {@link Strftime} formats, held the way C holds them so that the arithmetic ported from
 * glibc applies unchanged.
 *
 * <p>Two conventions come from {@code struct tm} rather than {@code java.time}: {@link #dayOfYear()}
 * counts from zero ({@code tm_yday}) and {@link #dayOfWeek()} counts from Sunday ({@code tm_wday}).
 * Every week-number formula in glibc is written against those, so converting them here keeps
 * {@link Strftime} a transcription rather than a reinterpretation.
 *
 * <p>Unlike a real {@code struct tm}, the three fields a calendar does not hand over for free —
 * the day of the year, the day of the week and the epoch second — are derived on first use, since
 * a pattern such as {@code "%Y-%m-%d"} asks for none of them.
 *
 * <p>Instances are mutable and meant to be filled once per format call. {@link #year} is proleptic,
 * not the {@code tm_year} offset from 1900.
 *
 * <p>Based on glibc:
 * <a href="https://sourceware.org/git/?p=glibc.git;a=blob;f=time/strftime_l.c;hb=HEAD#l548">__strftime_internal</a>,
 * which reads all of its input from a {@code struct tm} plus {@code tm_gmtoff} and the zone name.
 */
public final class BrokenDownTime {

    private static final int UNKNOWN = -1;

    /** Days before the first of each month in a non-leap year. */
    private static final int[] DAYS_BEFORE_MONTH =
            {0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334};

    int year;
    int month;
    int day;
    int hour;
    int minute;
    int second;

    /** Seconds east of UTC, as {@code tm_gmtoff}. */
    int offsetSeconds;

    private LocalDateTime local;
    private ZoneOffset offset;
    private ZoneId zone;

    /**
     * The input as a {@link ZonedDateTime}, needed only by the conversions that read locale
     * calendar data ({@code %x}, {@code %X}, {@code %Z} and the era forms).
     */
    private ZonedDateTime temporal;

    private int dayOfYear;
    private int dayOfWeek;
    private long epochSecond;
    private boolean epochKnown;

    BrokenDownTime() {
    }

    /**
     * Drops the references held on behalf of the last format call, so that a pooled instance does
     * not keep a caller's objects alive.
     */
    void clear() {
        this.local = null;
        this.offset = null;
        this.zone = null;
        this.temporal = null;
    }

    /**
     * Fills the fields from a zoned date-time, which already carries everything needed.
     */
    public void set(ZonedDateTime value) {
        setLocal(value.toLocalDateTime(), value.getOffset(), value.getZone());
        this.temporal = value;
    }

    /**
     * Fills the fields from an instant read in {@code zone}, without building a
     * {@link ZonedDateTime} unless one is later asked for.
     */
    public void set(Instant value, ZoneId zone) {
        ZoneOffset offset = zone.getRules().getOffset(value);
        long epochSecond = value.getEpochSecond();
        setLocal(LocalDateTime.ofEpochSecond(epochSecond, 0, offset), offset, zone);
        this.epochSecond = epochSecond;
        this.epochKnown = true;
    }

    /**
     * Fills the fields from an offset date-time, keeping its own offset rather than moving it to
     * another zone.
     */
    public void set(OffsetDateTime value) {
        setLocal(value.toLocalDateTime(), value.getOffset(), value.getOffset());
    }

    /**
     * Fills the fields from a legacy {@link Date} read in {@code zone}.
     */
    public void set(Date value, ZoneId zone) {
        set(value.toInstant(), zone);
    }

    /**
     * Fills the fields from a {@link Calendar} instant read in {@code zone}.
     */
    public void set(Calendar value, ZoneId zone) {
        set(value.toInstant(), zone);
    }

    /**
     * Fills the fields from a wall-clock reading interpreted in {@code zone}. A reading that falls
     * in a daylight-saving gap is moved forward by {@link LocalDateTime#atZone(ZoneId)}, as
     * elsewhere in this library.
     */
    public void set(LocalDateTime value, ZoneId zone) {
        set(value.atZone(zone));
    }

    /**
     * Fills the fields from the start of {@code value} in {@code zone}.
     */
    public void set(LocalDate value, ZoneId zone) {
        set(value.atStartOfDay(zone));
    }

    private void setLocal(LocalDateTime local, ZoneOffset offset, ZoneId zone) {
        this.local = local;
        this.offset = offset;
        this.zone = zone;
        this.offsetSeconds = offset.getTotalSeconds();

        this.year = local.getYear();
        this.month = local.getMonthValue();
        this.day = local.getDayOfMonth();
        this.hour = local.getHour();
        this.minute = local.getMinute();
        this.second = local.getSecond();

        this.temporal = null;
        this.dayOfYear = UNKNOWN;
        this.dayOfWeek = UNKNOWN;
        this.epochKnown = false;
    }

    /**
     * @return the day of the year counting from zero, as {@code tm_yday}
     */
    int dayOfYear() {
        int cached = dayOfYear;
        if (cached == UNKNOWN) {
            cached = DAYS_BEFORE_MONTH[month - 1] + day - 1;
            if (month > 2 && isLeapYear(year)) {
                cached++;
            }
            dayOfYear = cached;
        }
        return cached;
    }

    /**
     * @return the day of the week counting from Sunday, as {@code tm_wday}
     */
    int dayOfWeek() {
        int cached = dayOfWeek;
        if (cached == UNKNOWN) {
            // day zero of the epoch, 1970-01-01, was a Thursday, which tm_wday numbers 4
            cached = (int) Math.floorMod(local.toLocalDate().toEpochDay() + 4, 7);
            dayOfWeek = cached;
        }
        return cached;
    }

    long epochSecond() {
        if (!epochKnown) {
            epochSecond = local.toEpochSecond(offset);
            epochKnown = true;
        }
        return epochSecond;
    }

    /**
     * @return the input as a {@link ZonedDateTime}, rebuilding it on first use
     */
    ZonedDateTime temporal() {
        ZonedDateTime existing = temporal;
        if (existing == null) {
            // the offset is passed explicitly so an overlapping local time keeps the one it was read with
            existing = ZonedDateTime.ofInstant(local, offset, zone);
            temporal = existing;
        }
        return existing;
    }

    /**
     * @return the date alone, for era conversions that re-read it in another chronology
     */
    LocalDate toLocalDate() {
        return local.toLocalDate();
    }

    /**
     * @return the hour on a twelve-hour clock, as glibc's {@code hour12}
     */
    int hour12() {
        int rem = hour % 12;
        return rem == 0 ? 12 : rem;
    }

    static boolean isLeapYear(int year) {
        return (year & 3) == 0 && (year % 100 != 0 || year % 400 == 0);
    }
}
