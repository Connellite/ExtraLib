package io.github.connellite.format;

import io.github.connellite.exception.FormatException;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
                case 'Y' -> appendPadded(sb, zdt.getYear(), 4);
                case 'y' -> appendPadded(sb, Math.floorMod(zdt.getYear(), 100), 2);
                case 'm' -> appendPadded(sb, zdt.getMonthValue(), 2);
                case 'd' -> appendPadded(sb, zdt.getDayOfMonth(), 2);
                case 'H' -> appendPadded(sb, zdt.getHour(), 2);
                case 'M' -> appendPadded(sb, zdt.getMinute(), 2);
                case 'S' -> appendPadded(sb, zdt.getSecond(), 2);
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

    private static ZonedDateTime toZonedDateTime(Object value, ZoneId zone) {
        if (value instanceof ZonedDateTime z) {
            return z.withZoneSameInstant(zone);
        }
        if (value instanceof OffsetDateTime o) {
            return o.atZoneSameInstant(zone);
        }
        if (value instanceof Instant ins) {
            return ins.atZone(zone);
        }
        if (value instanceof Date d) {
            return d.toInstant().atZone(zone);
        }
        if (value instanceof Calendar cal) {
            return cal.toInstant().atZone(zone);
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt.atZone(zone);
        }
        if (value instanceof LocalDate ld) {
            return ld.atStartOfDay(zone);
        }
        return null;
    }
}
