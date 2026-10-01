package io.github.connellite.format.internal;

import io.github.connellite.exception.FormatException;
import io.github.connellite.util.DateTimeUtilFormat;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;

/**
 * Chrono replacement fields: {@code [[fill]align][width][.precision]} then strftime specs.
 *
 * <p>Based on
 * <a href="https://fmt.dev/11.2/syntax/#chrono-format-specifications">fmt 11.2 {@code chrono_format_spec}</a>
 * and
 * <a href="https://github.com/fmtlib/fmt/blob/11.2.0/include/fmt/chrono.h">fmt 11.2 {@code chrono.h}</a>.
 * Default {@code {}} is fmt {@code %F %T} (date-only types drop the time). {@link Duration} uses
 * tick count plus unit, or time-of-day conversions ({@code %H %M %S %T %R %j}) with glibc flags
 * forwarded to {@link DateTimeUtilFormat}; {@code %Q}/{@code %q} are ticks of Java's seconds/nanos
 * storage, not a C++ period type.
 */
@UtilityClass
final class ChronoFormatter {

    private static final LocalDate EPOCH_DATE = LocalDate.of(1970, 1, 1);

    static boolean isChronoValue(Object value) {
        return value instanceof Date
                || value instanceof Calendar
                || value instanceof Instant
                || value instanceof ZonedDateTime
                || value instanceof OffsetDateTime
                || value instanceof OffsetTime
                || value instanceof LocalDateTime
                || value instanceof LocalDate
                || value instanceof LocalTime
                || value instanceof Year
                || value instanceof YearMonth
                || value instanceof DayOfWeek
                || value instanceof Month
                || value instanceof Duration
                || value instanceof Period;
    }

    static boolean isChronoSpec(FormatSpec spec) {
        return spec != null && spec.hasRemaining() && spec.remaining().indexOf('%') >= 0;
    }

    static boolean isDefaultSpec(FormatSpec spec) {
        return spec != null
                && spec.type() == null
                && spec.special() == FormatSpec.Special.NONE
                && !spec.hasRemaining();
    }

    static void format(Appendable out, Object value, FormatSpec spec, java.util.Locale locale)
            throws IOException {
        String pattern = spec.remaining();
        String core;
        if (pattern == null || pattern.isEmpty() || pattern.indexOf('%') < 0) {
            core = defaultText(value, locale);
        } else if (value instanceof Duration d) {
            core = formatDuration(d, pattern, spec.precision(), locale);
        } else if (value instanceof Period p) {
            core = formatPeriod(p, pattern);
        } else {
            Object temporal = coerceTemporal(value);
            StringBuilder sb = new StringBuilder(pattern.length() + 16);
            DateTimeUtilFormat.strftimeTo(sb, locale, temporal, pattern);
            core = sb.toString();
        }
        out.append(Pad.apply(core, spec, Align.LEFT));
    }

    static void formatDefault(Appendable out, Object value, java.util.Locale locale) throws IOException {
        out.append(defaultText(value, locale));
    }

    static String defaultText(Object value, java.util.Locale locale) {
        if (value instanceof Duration d) {
            Ticks ticks = ticks(d.abs());
            return (d.isNegative() ? "-" : "") + ticks.count() + ticks.unit();
        }
        if (value instanceof Period p) {
            Ticks ticks = periodTicks(p);
            return ticks.count() + ticks.unit();
        }
        if (value instanceof Year y) {
            return DateTimeUtilFormat.strftime(locale, y, "%Y");
        }
        if (value instanceof YearMonth ym) {
            return DateTimeUtilFormat.strftime(locale, ym, "%Y-%m");
        }
        if (value instanceof LocalDate ld) {
            return DateTimeUtilFormat.strftime(locale, ld, "%F");
        }
        if (value instanceof LocalTime lt) {
            return formatClock(locale, EPOCH_DATE.atTime(lt), lt.getNano());
        }
        if (value instanceof OffsetTime ot) {
            return formatClock(locale, ot.atDate(EPOCH_DATE), ot.getNano());
        }
        if (value instanceof DayOfWeek dw) {
            return DateTimeUtilFormat.strftime(locale, EPOCH_DATE.with(dw), "%a");
        }
        if (value instanceof Month month) {
            return DateTimeUtilFormat.strftime(locale, YearMonth.of(1970, month).atDay(1), "%b");
        }
        if (value instanceof Instant instant) {
            return formatDateTime(locale, LocalDateTime.ofInstant(instant, ZoneOffset.UTC), instant.getNano());
        }
        Object temporal = coerceTemporal(value);
        int nano = nanoOf(value);
        if (temporal instanceof LocalDate ld) {
            return DateTimeUtilFormat.strftime(locale, ld, "%F");
        }
        return formatDateTime(locale, temporal, nano);
    }

    private static Object coerceTemporal(Object value) {
        if (value instanceof LocalTime lt) {
            return EPOCH_DATE.atTime(lt);
        }
        if (value instanceof OffsetTime ot) {
            return ot.atDate(EPOCH_DATE);
        }
        if (value instanceof Year y) {
            return y.atDay(1);
        }
        if (value instanceof YearMonth ym) {
            return ym.atDay(1);
        }
        if (value instanceof DayOfWeek dw) {
            return EPOCH_DATE.with(dw);
        }
        if (value instanceof Month month) {
            return YearMonth.of(1970, month).atDay(1);
        }
        return value;
    }

    private static int nanoOf(Object value) {
        if (value instanceof LocalDateTime ldt) {
            return ldt.getNano();
        }
        if (value instanceof OffsetDateTime odt) {
            return odt.getNano();
        }
        if (value instanceof ZonedDateTime zdt) {
            return zdt.getNano();
        }
        if (value instanceof Instant instant) {
            return instant.getNano();
        }
        if (value instanceof OffsetTime ot) {
            return ot.getNano();
        }
        if (value instanceof LocalTime lt) {
            return lt.getNano();
        }
        if (value instanceof Date d) {
            return d.toInstant().getNano();
        }
        if (value instanceof Calendar cal) {
            return cal.toInstant().getNano();
        }
        return 0;
    }

    private static String formatDateTime(java.util.Locale locale, Object temporal, int nano) {
        String date = DateTimeUtilFormat.strftime(locale, temporal, "%F");
        return date + " " + formatClock(locale, temporal, nano);
    }

    private static String formatClock(java.util.Locale locale, Object temporal, int nano) {
        String time = DateTimeUtilFormat.strftime(locale, temporal, "%T");
        if (nano == 0) {
            return time;
        }
        StringBuilder sb = new StringBuilder(time);
        appendFraction(sb, nano, -1);
        return sb.toString();
    }

    private static String formatDuration(Duration d, String pattern, int precision,
                                         java.util.Locale locale) {
        Duration abs = d.abs();
        boolean negative = d.isNegative();
        long totalSeconds = abs.getSeconds();
        int nano = abs.getNano();
        int hour = (int) ((totalSeconds / 3600L) % 24L);
        int minute = (int) ((totalSeconds / 60L) % 60L);
        int second = (int) (totalSeconds % 60L);
        long days = totalSeconds / 86_400L;
        LocalDateTime dummy = EPOCH_DATE.atTime(hour, minute, second);
        Ticks ticks = ticks(abs);

        StringBuilder sb = new StringBuilder(pattern.length() + 16);
        boolean signPending = negative;
        int n = pattern.length();
        int i = 0;
        while (i < n) {
            char c = pattern.charAt(i);
            if (c != '%') {
                sb.append(c);
                i++;
                continue;
            }
            int specStart = i;
            i++;
            if (i >= n) {
                sb.append('%');
                break;
            }
            StringBuilder flags = new StringBuilder();
            while (i < n) {
                char f = pattern.charAt(i);
                if (f == '-' || f == '_' || f == '0' || f == '+' || f == '^' || f == '#') {
                    flags.append(f);
                    i++;
                } else {
                    break;
                }
            }
            int widthStart = i;
            while (i < n && pattern.charAt(i) >= '0' && pattern.charAt(i) <= '9') {
                i++;
            }
            String width = pattern.substring(widthStart, i);
            char modifier = 0;
            if (i < n) {
                char m = pattern.charAt(i);
                if (m == 'E' || m == 'O') {
                    modifier = m;
                    i++;
                }
            }
            if (i >= n) {
                sb.append(pattern, specStart, n);
                break;
            }
            char conv = pattern.charAt(i++);
            if (conv == '%') {
                sb.append('%');
                continue;
            }
            if (conv == 'n') {
                sb.append('\n');
                continue;
            }
            if (conv == 't') {
                sb.append('\t');
                continue;
            }
            if (conv == 'Q') {
                if (modifier != 0) {
                    throw new FormatException("invalid format");
                }
                writeDurationSign(sb, signPending);
                signPending = false;
                sb.append(ticks.count());
                continue;
            }
            if (conv == 'q') {
                if (modifier != 0) {
                    throw new FormatException("invalid format");
                }
                sb.append(ticks.unit());
                continue;
            }
            if (isDateConversion(conv)) {
                throw new FormatException("no date");
            }
            if (conv == 'j') {
                writeDurationSign(sb, signPending);
                signPending = false;
                sb.append(days);
                continue;
            }
            if (!isTimeConversion(conv)) {
                throw new FormatException("invalid format specifier: %" + conv);
            }
            writeDurationSign(sb, signPending);
            signPending = false;
            String piece = "%" + flags + width + (modifier == 0 ? "" : String.valueOf(modifier)) + conv;
            String rendered = DateTimeUtilFormat.strftime(locale, dummy, piece);
            sb.append(rendered);
            if (conv == 'S' || conv == 'T') {
                appendFraction(sb, nano, precision);
            }
        }
        return sb.toString();
    }

    private static void writeDurationSign(StringBuilder sb, boolean signPending) {
        if (signPending) {
            sb.append('-');
        }
    }

    private static boolean isTimeConversion(char conv) {
        return conv == 'H' || conv == 'I' || conv == 'M' || conv == 'S'
                || conv == 'R' || conv == 'T' || conv == 'r'
                || conv == 'p' || conv == 'P' || conv == 'k' || conv == 'l';
    }

    private static boolean isDateConversion(char conv) {
        return conv == 'a' || conv == 'A' || conv == 'b' || conv == 'B' || conv == 'h'
                || conv == 'c' || conv == 'x' || conv == 'X' || conv == 'D' || conv == 'F'
                || conv == 'w' || conv == 'u' || conv == 'U' || conv == 'W' || conv == 'V'
                || conv == 'z' || conv == 'Z' || conv == 'Y' || conv == 'y' || conv == 'C'
                || conv == 'G' || conv == 'g' || conv == 'm' || conv == 'd' || conv == 'e'
                || conv == 's';
    }

    private static Ticks ticks(Duration abs) {
        if (abs.getNano() == 0) {
            return new Ticks(abs.getSeconds(), "s");
        }
        long nanos = abs.toNanos();
        if (nanos % 1_000_000L == 0) {
            return new Ticks(nanos / 1_000_000L, "ms");
        }
        if (nanos % 1_000L == 0) {
            return new Ticks(nanos / 1_000L, "us");
        }
        return new Ticks(nanos, "ns");
    }

    private static String formatPeriod(Period p, String pattern) {
        Ticks ticks = periodTicks(p);
        return expandQq(pattern, Long.toString(ticks.count()), ticks.unit(), "period");
    }

    private static Ticks periodTicks(Period p) {
        if (p.getYears() != 0 && p.getMonths() == 0 && p.getDays() == 0) {
            return new Ticks(p.getYears(), "y");
        }
        if (p.getMonths() != 0 && p.getDays() == 0 && p.getYears() == 0) {
            return new Ticks(p.getMonths(), "m");
        }
        return new Ticks(p.getDays(), "d");
    }

    private static String expandQq(String pattern, String count, String unit, String kind) {
        StringBuilder sb = new StringBuilder(pattern.length() + 8);
        int n = pattern.length();
        int i = 0;
        while (i < n) {
            char c = pattern.charAt(i);
            if (c == '%' && i + 1 < n) {
                char next = pattern.charAt(i + 1);
                if (next == 'Q') {
                    sb.append(count);
                    i += 2;
                    continue;
                }
                if (next == 'q') {
                    sb.append(unit);
                    i += 2;
                    continue;
                }
                if (next == '%') {
                    sb.append('%');
                    i += 2;
                    continue;
                }
                throw new FormatException("chrono conversion %" + next + " is not supported for " + kind);
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    private static void appendFraction(StringBuilder sb, int nano, int precision) {
        if (nano < 0) {
            nano = 0;
        }
        String digits = pad9(nano);
        if (precision == 0) {
            return;
        }
        if (precision > 0) {
            if (precision <= 9) {
                sb.append('.').append(digits, 0, precision);
            } else {
                sb.append('.').append(digits);
                sb.append("0".repeat(precision - 9));
            }
            return;
        }
        int end = 9;
        while (end > 0 && digits.charAt(end - 1) == '0') {
            end--;
        }
        if (end == 0) {
            return;
        }
        sb.append('.').append(digits, 0, end);
    }

    private static String pad9(int nano) {
        String raw = Integer.toString(nano);
        if (raw.length() >= 9) {
            return raw;
        }
        return "0".repeat(9 - raw.length()) + raw;
    }

    private record Ticks(long count, String unit) {
    }
}
