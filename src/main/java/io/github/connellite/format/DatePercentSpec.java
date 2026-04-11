package io.github.connellite.format;

import io.github.connellite.exception.FormatException;
import io.github.connellite.util.DateTimeUtil;
import lombok.experimental.UtilityClass;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/**
 * strftime-like subset after {@code ':'} when the spec contains {@code %}, e.g.
 * {@code %Y-%m-%d %H:%M:%S} for {@link java.util.Date} and {@link java.time} types.
 */
@UtilityClass
final class DatePercentSpec {

    static String format(Locale locale, Object value, String spec) {
        Objects.requireNonNull(locale, "locale");
        if (value == null) {
            return "null";
        }
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime zdt = toZonedDateTime(value, zone);
        if (zdt == null) {
            throw new FormatException("value type not supported for date spec: " + spec);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < spec.length(); i++) {
            char c = spec.charAt(i);
            if (c != '%' || i + 1 >= spec.length()) {
                sb.append(c);
                continue;
            }
            char d = spec.charAt(++i);
            switch (d) {
                case '%' -> sb.append('%');
                case 'a' -> sb.append(DateTimeFormatter.ofPattern("EEE", locale).format(zdt));
                case 'A' -> sb.append(DateTimeFormatter.ofPattern("EEEE", locale).format(zdt));
                case 'b', 'h' -> sb.append(DateTimeFormatter.ofPattern("MMM", locale).format(zdt));
                case 'B' -> sb.append(DateTimeFormatter.ofPattern("MMMM", locale).format(zdt));
                case 'c' -> sb.append(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale).format(zdt));
                case 'C' -> appendPadded(sb, Math.floorDiv(zdt.getYear(), 100), 2);
                case 'd' -> appendPadded(sb, zdt.getDayOfMonth(), 2);
                case 'D' -> sb.append(format(locale, zdt, "%m/%d/%y"));
                case 'e' -> appendSpacePadded(sb, zdt.getDayOfMonth(), 2);
                case 'F' -> sb.append(format(locale, zdt, "%Y-%m-%d"));
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
                case 'r' -> sb.append(format(locale, zdt, "%I:%M:%S %p"));
                case 'R' -> sb.append(format(locale, zdt, "%H:%M"));
                case 'S' -> appendPadded(sb, zdt.getSecond(), 2);
                case 'T' -> sb.append(format(locale, zdt, "%H:%M:%S"));
                case 'u' -> sb.append(zdt.getDayOfWeek().getValue());
                case 'U' -> appendPadded(sb, weekNumberSundayFirst(zdt.toLocalDate()), 2);
                case 'V' -> appendPadded(sb, zdt.get(WeekFields.ISO.weekOfWeekBasedYear()), 2);
                case 'w' -> sb.append(zdt.getDayOfWeek() == DayOfWeek.SUNDAY ? 0 : zdt.getDayOfWeek().getValue());
                case 'W' -> appendPadded(sb, weekNumberMondayFirst(zdt.toLocalDate()), 2);
                case 'x' -> sb.append(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale).format(zdt));
                case 'X' -> sb.append(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).withLocale(locale).format(zdt));
                case 'y' -> appendPadded(sb, Math.floorMod(zdt.getYear(), 100), 2);
                case 'Y' -> appendPadded(sb, zdt.getYear(), 4);
                case 'z' -> sb.append(DateTimeFormatter.ofPattern("xx", locale).format(zdt));
                case 'Z' -> sb.append(DateTimeFormatter.ofPattern("z", locale).format(zdt));
                default -> throw new FormatException("unknown date conversion: %" + d);
            }
        }
        return sb.toString();
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

    private static ZonedDateTime toZonedDateTime(Object value, ZoneId zone) {
        if (value instanceof ZonedDateTime z) {
            return DateTimeUtil.toZonedDateTime(z, zone);
        }
        if (value instanceof OffsetDateTime o) {
            return DateTimeUtil.toZonedDateTime(o, zone);
        }
        if (value instanceof Instant ins) {
            return DateTimeUtil.toZonedDateTime(ins, zone);
        }
        if (value instanceof Date d) {
            return DateTimeUtil.toZonedDateTime(d, zone);
        }
        if (value instanceof Calendar cal) {
            return DateTimeUtil.toZonedDateTime(cal, zone);
        }
        if (value instanceof LocalDateTime ldt) {
            return DateTimeUtil.toZonedDateTime(ldt, zone);
        }
        if (value instanceof LocalDate ld) {
            return DateTimeUtil.toZonedDateTime(ld, zone);
        }
        return null;
    }
}
