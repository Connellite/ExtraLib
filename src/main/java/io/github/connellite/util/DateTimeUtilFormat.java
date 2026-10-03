package io.github.connellite.util;

import io.github.connellite.exception.FormatException;
import io.github.connellite.util.internal.BrokenDownTime;
import io.github.connellite.util.internal.Strftime;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * POSIX {@code strftime} formatting for {@link java.time} and legacy date types.
 * <p>
 * The conversions are produced by {@link Strftime}, a port of the glibc engine, so the output
 * matches a C library byte for byte wherever the JDK exposes the same locale data. See that class
 * for the three places where it cannot and what it does instead.
 * </p>
 */
@SuppressWarnings("SpellCheckingInspection")
@UtilityClass
public class DateTimeUtilFormat {

    private static final class SystemDefaultZoneHolder {
        private static final ZoneId INSTANCE = ZoneId.systemDefault();
    }

    private static final class StrftimeEngineHolder {
        private static final ThreadLocal<Strftime> ENGINE = ThreadLocal.withInitial(Strftime::new);
    }

    /**
     * Formats a date-time using POSIX {@code strftime} conversion specifiers.
     * <p>
     * A specifier is {@code '%'}, an optional flag out of {@code - _ 0 + ^ #}, an optional minimum
     * field width, an optional {@code E} or {@code O} modifier, and the conversion character. The
     * POSIX set {@code %a %A %b %B %c %C %d %D %e %F %g %G %h %H %I %j %m %M %n %p %r %R %S %t %T
     * %u %U %V %w %W %x %X %y %Y %z %Z %%} is supported, as are the GNU extensions {@code %k %l %P
     * %s}. An unknown conversion is echoed back literally, including the {@code '%'}, as glibc
     * does.
     * </p>
     * <p>
     * {@code E} selects the locale's alternative calendar, which the JDK provides for locales
     * carrying a {@code ca} extension such as {@code ja-JP-u-ca-japanese}; {@code O} selects the
     * locale's alternative digits. Where the locale has neither, the conversion behaves as if the
     * modifier were absent, which is what POSIX requires.
     * </p>
     * <p>
     * {@code %z} is the offset from UTC in ISO 8601 <em>basic</em> form {@code ±hhmm} (no colon).
     * {@code %Z} is the time-zone abbreviation or short name from the JDK formatter pattern
     * {@code z}. {@code %c} follows the common C-library layout {@code %a %b %e %H:%M:%S %Y}, while
     * {@code %x} and {@code %X} are localized. Week-based specifiers {@code %g %G %V} follow ISO
     * week date rules.
     * </p>
     *
     * @param locale  locale for localized elements; must not be {@code null}
     * @param zdt     the zoned date-time, or {@code null} (treated as the literal string {@code "null"})
     * @param pattern format string with {@code %} conversions; must not be {@code null}
     * @return formatted string
     */
    public static String strftime(@NonNull Locale locale, ZonedDateTime zdt, @NonNull String pattern) {
        if (zdt == null) {
            return "null";
        }
        Strftime engine = engine();
        engine.time().set(zdt);
        return render(engine, locale, pattern);
    }

    /**
     * Same as {@link #strftime(Locale, ZonedDateTime, String)} using {@code instant} in the
     * {@linkplain ZoneId#systemDefault() system default} time zone.
     */
    public static String strftime(@NonNull Locale locale, Instant instant, @NonNull String pattern) {
        return strftime(locale, instant, SystemDefaultZoneHolder.INSTANCE, pattern);
    }

    /**
     * {@code instant} at {@code zone}, then {@link #strftime(Locale, ZonedDateTime, String)}.
     */
    public static String strftime(@NonNull Locale locale, Instant instant, ZoneId zone, @NonNull String pattern) {
        if (instant == null) {
            return "null";
        }
        Strftime engine = engine();
        engine.time().set(instant, zone);
        return render(engine, locale, pattern);
    }

    /**
     * Same as {@link #strftime(Locale, ZonedDateTime, String)} using {@code odt} at the same instant in the
     * {@linkplain ZoneId#systemDefault() system default} time zone.
     */
    public static String strftime(@NonNull Locale locale, OffsetDateTime odt, @NonNull String pattern) {
        return strftime(locale, odt, SystemDefaultZoneHolder.INSTANCE, pattern);
    }

    /**
     * {@code odt} at the same instant in {@code zone}, then {@link #strftime(Locale, ZonedDateTime, String)}.
     */
    public static String strftime(@NonNull Locale locale, OffsetDateTime odt, ZoneId zone, @NonNull String pattern) {
        if (odt == null) {
            return "null";
        }
        Strftime engine = engine();
        engine.time().set(odt.toInstant(), zone);
        return render(engine, locale, pattern);
    }

    /**
     * Same as {@link #strftime(Locale, ZonedDateTime, String)} using {@code date} in the
     * {@linkplain ZoneId#systemDefault() system default} time zone.
     */
    public static String strftime(@NonNull Locale locale, Date date, @NonNull String pattern) {
        return strftime(locale, date, SystemDefaultZoneHolder.INSTANCE, pattern);
    }

    /**
     * {@code date} at the same instant in {@code zone}, then {@link #strftime(Locale, ZonedDateTime, String)}.
     */
    public static String strftime(@NonNull Locale locale, Date date, ZoneId zone, @NonNull String pattern) {
        if (date == null) {
            return "null";
        }
        Strftime engine = engine();
        engine.time().set(date, zone);
        return render(engine, locale, pattern);
    }

    /**
     * Same as {@link #strftime(Locale, ZonedDateTime, String)} using the calendar instant in the
     * {@linkplain ZoneId#systemDefault() system default} time zone (same instant on the time-line).
     */
    public static String strftime(@NonNull Locale locale, Calendar calendar, @NonNull String pattern) {
        return strftime(locale, calendar, SystemDefaultZoneHolder.INSTANCE, pattern);
    }

    /**
     * {@code calendar} at the same instant in {@code zone}, then {@link #strftime(Locale, ZonedDateTime, String)}.
     */
    public static String strftime(@NonNull Locale locale, Calendar calendar, ZoneId zone, @NonNull String pattern) {
        if (calendar == null) {
            return "null";
        }
        Strftime engine = engine();
        engine.time().set(calendar, zone);
        return render(engine, locale, pattern);
    }

    /**
     * {@code ldt} interpreted as local clock in the {@linkplain ZoneId#systemDefault() system default} zone,
     * then {@link #strftime(Locale, ZonedDateTime, String)}.
     */
    public static String strftime(@NonNull Locale locale, LocalDateTime ldt, @NonNull String pattern) {
        return strftime(locale, ldt, SystemDefaultZoneHolder.INSTANCE, pattern);
    }

    /**
     * {@code ldt} interpreted as local clock in {@code zone}, then {@link #strftime(Locale, ZonedDateTime, String)}.
     */
    public static String strftime(@NonNull Locale locale, LocalDateTime ldt, ZoneId zone, @NonNull String pattern) {
        if (ldt == null) {
            return "null";
        }
        Strftime engine = engine();
        engine.time().set(ldt, zone);
        return render(engine, locale, pattern);
    }

    /**
     * Start of {@code ld} in the {@linkplain ZoneId#systemDefault() system default} zone,
     * then {@link #strftime(Locale, ZonedDateTime, String)}.
     */
    public static String strftime(@NonNull Locale locale, LocalDate ld, @NonNull String pattern) {
        return strftime(locale, ld, SystemDefaultZoneHolder.INSTANCE, pattern);
    }

    /**
     * Start of {@code ld} at {@code zone}, then {@link #strftime(Locale, ZonedDateTime, String)}.
     */
    public static String strftime(@NonNull Locale locale, LocalDate ld, ZoneId zone, @NonNull String pattern) {
        if (ld == null) {
            return "null";
        }
        Strftime engine = engine();
        engine.time().set(ld, zone);
        return render(engine, locale, pattern);
    }

    /**
     * Formats a supported date-time {@code value} for {@code Fmt} and similar APIs.
     * {@link ZonedDateTime} is left unchanged; {@link OffsetDateTime} keeps its fixed offset. All other
     * supported types are interpreted in the {@linkplain ZoneId#systemDefault() system default} zone.
     * <p>
     * Supported types: {@link ZonedDateTime}, {@link OffsetDateTime}, {@link Instant}, {@link Date},
     * {@link Calendar}, {@link LocalDateTime}, {@link LocalDate}, {@link LocalTime},
     * {@link OffsetTime}, {@link Year}, {@link YearMonth}. A {@code null} value yields the literal
     * {@code "null"}; an unsupported type throws {@link FormatException}.
     * </p>
     */
    public static String strftime(@NonNull Locale locale, Object value, @NonNull String pattern) {
        if (value == null) {
            return "null";
        }
        Strftime engine = engine();
        fill(engine.time(), value);
        return render(engine, locale, pattern);
    }

    /**
     * Formats {@code value} straight into {@code out}, for callers that already hold the buffer the
     * result is headed for.
     *
     * @throws FormatException if {@code value} is of an unsupported type
     * @see #strftime(Locale, Object, String)
     */
    public static void strftimeTo(@NonNull Appendable out, @NonNull Locale locale, Object value,
                                  @NonNull String pattern) {
        if (out instanceof StringBuilder sb) {
            if (value == null) {
                sb.append("null");
                return;
            }
            Strftime engine = engine();
            try {
                fill(engine.time(), value);
                engine.format(sb, pattern, locale);
            } finally {
                engine.clear();
            }
            return;
        }
        try {
            out.append(strftime(locale, value, pattern));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static void fill(BrokenDownTime time, Object value) {
        ZoneId zone = SystemDefaultZoneHolder.INSTANCE;
        if (value instanceof ZonedDateTime z) {
            time.set(z);
        } else if (value instanceof OffsetDateTime o) {
            time.set(o);
        } else if (value instanceof Instant instant) {
            time.set(instant, zone);
        } else if (value instanceof Date d) {
            time.set(d, zone);
        } else if (value instanceof Calendar cal) {
            time.set(cal, zone);
        } else if (value instanceof LocalDateTime ldt) {
            time.set(ldt, zone);
        } else if (value instanceof LocalDate ld) {
            time.set(ld, zone);
        } else if (value instanceof LocalTime lt) {
            time.set(LocalDate.of(1970, 1, 1).atTime(lt), zone);
        } else if (value instanceof OffsetTime ot) {
            time.set(ot.atDate(LocalDate.of(1970, 1, 1)));
        } else if (value instanceof Year y) {
            time.set(y.atDay(1), zone);
        } else if (value instanceof YearMonth ym) {
            time.set(ym.atDay(1), zone);
        } else {
            throw new FormatException("value type not supported for strftime: " + value.getClass().getName());
        }
    }

    private static Strftime engine() {
        return StrftimeEngineHolder.ENGINE.get();
    }

    private static String render(Strftime engine, Locale locale, String pattern) {
        try {
            StringBuilder sb = new StringBuilder(pattern.length() + 16);
            engine.format(sb, pattern, locale);
            return sb.toString();
        } finally {
            engine.clear();
        }
    }
}
