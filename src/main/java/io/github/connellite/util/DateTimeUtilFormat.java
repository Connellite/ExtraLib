package io.github.connellite.util;

import io.github.connellite.exception.FormatException;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * POSIX/C99-style {@code strftime} formatting for {@link java.time} and legacy date types,
 * built on top of {@link DateTimeUtil} conversions where needed.
 */
@UtilityClass
public class DateTimeUtilFormat {

    private static final class SystemDefaultZoneHolder {
        private static final ZoneId INSTANCE = ZoneId.systemDefault();
    }

    /**
     * Formats a date-time using a subset of C99 / POSIX {@code strftime} conversion specifiers.
     * <p>
     * Supported specifiers include {@code %a %A %b %B %c %C %d %D %e %F %g %G %H %h %I %j %m %M %n %p %r %R %S %T
     * %u %U %V %w %W %x %X %y %Y %z %Z %%}. Locale affects names where specified by POSIX (weekday/month names,
     * {@code %c %x %X %p}, and {@code %Z}). Week-based specifiers {@code %g %G %V} follow ISO week date rules.
     * </p>
     * <p>
     * {@code %z} is the offset from UTC in ISO 8601 <em>basic</em> form {@code ±hhmm} (no colon), as in POSIX.
     * {@code %Z} is the time-zone abbreviation or short name from the JDK formatter pattern {@code z}
     * (implementation-defined where no abbreviation exists, per POSIX).
     * </p>
     *
     * @param locale  locale for localized elements; must not be {@code null}
     * @param zdt     the zoned date-time, or {@code null} (treated as the literal string {@code "null"})
     * @param pattern format string with {@code %} conversions; must not be {@code null}
     * @return formatted string
     * @throws FormatException if the pattern contains an unknown {@code %} conversion
     */
    public static String strftime(@NonNull Locale locale, ZonedDateTime zdt, @NonNull String pattern) {
        if (zdt == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c != '%' || i + 1 >= pattern.length()) {
                sb.append(c);
                continue;
            }
            char d = pattern.charAt(++i);
            switch (d) {
                case '%' -> sb.append('%');
                case 'a' -> sb.append(DateTimeFormatter.ofPattern("EEE", locale).format(zdt));
                case 'A' -> sb.append(DateTimeFormatter.ofPattern("EEEE", locale).format(zdt));
                case 'b', 'h' -> sb.append(DateTimeFormatter.ofPattern("MMM", locale).format(zdt));
                case 'B' -> sb.append(DateTimeFormatter.ofPattern("MMMM", locale).format(zdt));
                case 'c' ->
                        sb.append(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale).format(zdt));
                case 'C' -> appendPadded(sb, Math.floorDiv(zdt.getYear(), 100), 2);
                case 'd' -> appendPadded(sb, zdt.getDayOfMonth(), 2);
                case 'D' -> sb.append(strftime(locale, zdt, "%m/%d/%y"));
                case 'e' -> appendSpacePadded(sb, zdt.getDayOfMonth(), 2);
                case 'F' -> sb.append(strftime(locale, zdt, "%Y-%m-%d"));
                case 'g' -> appendPadded(sb, Math.floorMod(zdt.get(WeekFields.ISO.weekBasedYear()), 100), 2);
                case 'G' -> appendPadded(sb, zdt.get(WeekFields.ISO.weekBasedYear()), 4);
                case 'H' -> appendPadded(sb, zdt.getHour(), 2);
                case 'I' -> {
                    int hour = zdt.getHour() % 12;
                    appendPadded(sb, hour == 0 ? 12 : hour, 2);
                }
                case 'j' -> appendPadded(sb, zdt.getDayOfYear(), 3);
                case 'm' -> appendPadded(sb, zdt.getMonthValue(), 2);
                case 'M' -> appendPadded(sb, zdt.getMinute(), 2);
                case 'n' -> sb.append('\n');
                case 'p' -> sb.append(DateTimeFormatter.ofPattern("a", locale).format(zdt));
                case 'r' -> sb.append(strftime(locale, zdt, "%I:%M:%S %p"));
                case 'R' -> sb.append(strftime(locale, zdt, "%H:%M"));
                case 'S' -> appendPadded(sb, zdt.getSecond(), 2);
                case 'T' -> sb.append(strftime(locale, zdt, "%H:%M:%S"));
                case 'u' -> sb.append(zdt.getDayOfWeek().getValue());
                case 'U' -> appendPadded(sb, weekNumberSundayFirst(zdt.toLocalDate()), 2);
                case 'V' -> appendPadded(sb, zdt.get(WeekFields.ISO.weekOfWeekBasedYear()), 2);
                case 'w' -> sb.append(zdt.getDayOfWeek() == DayOfWeek.SUNDAY ? 0 : zdt.getDayOfWeek().getValue());
                case 'W' -> appendPadded(sb, weekNumberMondayFirst(zdt.toLocalDate()), 2);
                case 'x' ->
                        sb.append(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale).format(zdt));
                case 'X' ->
                        sb.append(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).withLocale(locale).format(zdt));
                case 'y' -> appendPadded(sb, Math.floorMod(zdt.getYear(), 100), 2);
                case 'Y' -> appendPadded(sb, zdt.getYear(), 4);
                case 'z' -> appendStrftimePercentZ(sb, zdt);
                case 'Z' -> sb.append(DateTimeFormatter.ofPattern("z", locale).format(zdt));
                default -> throw new FormatException("unknown strftime conversion: %" + d);
            }
        }
        return sb.toString();
    }

    /**
     * Same as {@link #strftime(Locale, ZonedDateTime, String)} using {@code instant} in the
     * {@linkplain ZoneId#systemDefault() system default} time zone.
     */
    public static String strftime(@NonNull Locale locale, Instant instant, @NonNull String pattern) {
        if (instant == null) {
            return "null";
        }
        return strftime(locale, DateTimeUtil.toZonedDateTime(instant, SystemDefaultZoneHolder.INSTANCE), pattern);
    }

    /**
     * {@code instant} at {@code zone}, then {@link #strftime(Locale, ZonedDateTime, String)}.
     */
    public static String strftime(@NonNull Locale locale, Instant instant, ZoneId zone, @NonNull String pattern) {
        if (instant == null) {
            return "null";
        }
        return strftime(locale, DateTimeUtil.toZonedDateTime(instant, SystemDefaultZoneHolder.INSTANCE), pattern);
    }

    /**
     * Same as {@link #strftime(Locale, ZonedDateTime, String)} using {@code odt} at the same instant in the
     * {@linkplain ZoneId#systemDefault() system default} time zone.
     */
    public static String strftime(@NonNull Locale locale, OffsetDateTime odt, @NonNull String pattern) {
        if (odt == null) {
            return "null";
        }
        return strftime(locale, DateTimeUtil.toZonedDateTime(odt, SystemDefaultZoneHolder.INSTANCE), pattern);
    }

    /**
     * {@code odt} at the same instant in {@code zone}, then {@link #strftime(Locale, ZonedDateTime, String)}.
     */
    public static String strftime(@NonNull Locale locale, OffsetDateTime odt, ZoneId zone, @NonNull String pattern) {
        if (odt == null) {
            return "null";
        }
        return strftime(locale, DateTimeUtil.toZonedDateTime(odt, SystemDefaultZoneHolder.INSTANCE), pattern);
    }

    /**
     * Same as {@link #strftime(Locale, ZonedDateTime, String)} using {@code date} in the
     * {@linkplain ZoneId#systemDefault() system default} time zone.
     */
    public static String strftime(@NonNull Locale locale, Date date, @NonNull String pattern) {
        if (date == null) {
            return "null";
        }
        return strftime(locale, DateTimeUtil.toZonedDateTime(date, SystemDefaultZoneHolder.INSTANCE), pattern);
    }

    /**
     * {@code date} at the same instant in {@code zone}, then {@link #strftime(Locale, ZonedDateTime, String)}.
     */
    public static String strftime(@NonNull Locale locale, Date date, ZoneId zone, @NonNull String pattern) {
        if (date == null) {
            return "null";
        }
        return strftime(locale, DateTimeUtil.toZonedDateTime(date, SystemDefaultZoneHolder.INSTANCE), pattern);
    }

    /**
     * Same as {@link #strftime(Locale, ZonedDateTime, String)} using the calendar instant in the
     * {@linkplain ZoneId#systemDefault() system default} time zone (same instant on the time-line).
     */
    public static String strftime(@NonNull Locale locale, Calendar calendar, @NonNull String pattern) {
        if (calendar == null) {
            return "null";
        }
        return strftime(locale, DateTimeUtil.toZonedDateTime(calendar, SystemDefaultZoneHolder.INSTANCE), pattern);
    }

    /**
     * {@code calendar} at the same instant in {@code zone}, then {@link #strftime(Locale, ZonedDateTime, String)}.
     */
    public static String strftime(@NonNull Locale locale, Calendar calendar, ZoneId zone, @NonNull String pattern) {
        if (calendar == null) {
            return "null";
        }
        return strftime(locale, DateTimeUtil.toZonedDateTime(calendar, SystemDefaultZoneHolder.INSTANCE), pattern);
    }

    /**
     * {@code ldt} interpreted as local clock in the {@linkplain ZoneId#systemDefault() system default} zone,
     * then {@link #strftime(Locale, ZonedDateTime, String)}.
     */
    public static String strftime(@NonNull Locale locale, LocalDateTime ldt, @NonNull String pattern) {
        if (ldt == null) {
            return "null";
        }
        return strftime(locale, DateTimeUtil.toZonedDateTime(ldt, SystemDefaultZoneHolder.INSTANCE), pattern);
    }

    /**
     * {@code ldt} interpreted as local clock in {@code zone}, then {@link #strftime(Locale, ZonedDateTime, String)}.
     */
    public static String strftime(@NonNull Locale locale, LocalDateTime ldt, ZoneId zone, @NonNull String pattern) {
        if (ldt == null) {
            return "null";
        }
        return strftime(locale, DateTimeUtil.toZonedDateTime(ldt, SystemDefaultZoneHolder.INSTANCE), pattern);
    }

    /**
     * Start of {@code ld} in the {@linkplain ZoneId#systemDefault() system default} zone,
     * then {@link #strftime(Locale, ZonedDateTime, String)}.
     */
    public static String strftime(@NonNull Locale locale, LocalDate ld, @NonNull String pattern) {
        if (ld == null) {
            return "null";
        }
        return strftime(locale, DateTimeUtil.toZonedDateTime(ld, SystemDefaultZoneHolder.INSTANCE), pattern);
    }

    /**
     * Start of {@code ld} at {@code zone}, then {@link #strftime(Locale, ZonedDateTime, String)}.
     */
    public static String strftime(@NonNull Locale locale, LocalDate ld, ZoneId zone, @NonNull String pattern) {
        if (ld == null) {
            return "null";
        }
        return strftime(locale, DateTimeUtil.toZonedDateTime(ld, SystemDefaultZoneHolder.INSTANCE), pattern);
    }

    /**
     * Formats a supported date-time {@code value} for {@code Fmt} and similar APIs: converts to
     * {@link ZonedDateTime} in the {@linkplain ZoneId#systemDefault() system default} zone, then
     * {@link #strftime(Locale, ZonedDateTime, String)}.
     * <p>
     * Supported types: {@link ZonedDateTime}, {@link OffsetDateTime}, {@link Instant}, {@link Date},
     * {@link Calendar}, {@link LocalDateTime}, {@link LocalDate}. A {@code null} value yields the literal
     * {@code "null"}; an unsupported type throws {@link FormatException}.
     * </p>
     */
    public static String strftime(@NonNull Locale locale, Object value, @NonNull String pattern) {
        if (value == null) {
            return "null";
        }
        ZonedDateTime zdt = strftimeValueToZonedDateTime(value);
        if (zdt == null) {
            throw new FormatException("value type not supported for strftime: " + value.getClass().getName());
        }
        return strftime(locale, zdt, pattern);
    }

    private static ZonedDateTime strftimeValueToZonedDateTime(Object value) {
        if (value instanceof ZonedDateTime z) {
            return DateTimeUtil.toZonedDateTime(z, SystemDefaultZoneHolder.INSTANCE);
        }
        if (value instanceof OffsetDateTime o) {
            return DateTimeUtil.toZonedDateTime(o, SystemDefaultZoneHolder.INSTANCE);
        }
        if (value instanceof Instant ins) {
            return DateTimeUtil.toZonedDateTime(ins, SystemDefaultZoneHolder.INSTANCE);
        }
        if (value instanceof Date d) {
            return DateTimeUtil.toZonedDateTime(d, SystemDefaultZoneHolder.INSTANCE);
        }
        if (value instanceof Calendar cal) {
            return DateTimeUtil.toZonedDateTime(cal, SystemDefaultZoneHolder.INSTANCE);
        }
        if (value instanceof LocalDateTime ldt) {
            return DateTimeUtil.toZonedDateTime(ldt, SystemDefaultZoneHolder.INSTANCE);
        }
        if (value instanceof LocalDate ld) {
            return DateTimeUtil.toZonedDateTime(ld, SystemDefaultZoneHolder.INSTANCE);
        }
        return null;
    }

    private static void appendStrftimePercentZ(StringBuilder sb, ZonedDateTime zdt) {
        ZoneOffset offset = zdt.getOffset();
        int totalSeconds = offset.getTotalSeconds();
        sb.append(totalSeconds >= 0 ? '+' : '-');
        int abs = Math.abs(totalSeconds);
        int hours = abs / 3600;
        int minutes = (abs % 3600) / 60;
        appendPadded(sb, hours, 2);
        appendPadded(sb, minutes, 2);
    }

    private static void appendPadded(StringBuilder sb, int v, int width) {
        String s = Integer.toString(v);
        sb.append("0".repeat(Math.max(0, width - s.length())));
        sb.append(s);
    }

    private static void appendSpacePadded(StringBuilder sb, int v, int width) {
        String s = Integer.toString(v);
        sb.append(" ".repeat(Math.max(0, width - s.length())));
        sb.append(s);
    }

    private static int weekNumberSundayFirst(LocalDate date) {
        LocalDate jan1 = date.withDayOfYear(1);
        LocalDate firstSunday = jan1.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        if (date.isBefore(firstSunday)) {
            return 0;
        }
        return (int) ((date.toEpochDay() - firstSunday.toEpochDay()) / 7) + 1;
    }

    private static int weekNumberMondayFirst(LocalDate date) {
        LocalDate jan1 = date.withDayOfYear(1);
        LocalDate firstMonday = jan1.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        if (date.isBefore(firstMonday)) {
            return 0;
        }
        return (int) ((date.toEpochDay() - firstMonday.toEpochDay()) / 7) + 1;
    }
}
