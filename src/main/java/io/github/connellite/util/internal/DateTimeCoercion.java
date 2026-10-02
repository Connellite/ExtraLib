package io.github.connellite.util.internal;

import io.github.connellite.exception.TypeCoercionException;
import io.github.connellite.util.DateTimeUtil;
import io.github.connellite.util.NumberUtils;
import io.github.connellite.util.StringUtils;
import io.github.connellite.util.TypeCoercionUtil;
import lombok.experimental.UtilityClass;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

/**
 * Date and time coercion used by {@link TypeCoercionUtil}.
 */
@UtilityClass
public class DateTimeCoercion {

    private static final Set<?> DATE_TIME_TYPES = Set.of(
            java.sql.Date.class,
            Time.class,
            Timestamp.class,
            Date.class,
            LocalDate.class,
            LocalTime.class,
            LocalDateTime.class,
            Instant.class,
            ZonedDateTime.class,
            OffsetDateTime.class,
            Year.class,
            YearMonth.class,
            MonthDay.class,
            OffsetTime.class,
            ZoneId.class,
            ZoneOffset.class
    );

    /**
     * Returns whether {@code targetType} is a date or time type handled here.
     *
     * @param targetType desired type, never {@code null}
     * @return {@code true} for JDBC, {@link Date}, and {@code java.time} date/time targets
     */
    public static boolean supports(Class<?> targetType) {
        return DATE_TIME_TYPES.contains(targetType);
    }

    /**
     * Coerces {@code raw} toward a date or time {@code targetType}.
     * {@link #supports(Class)} is {@code true} for {@code targetType}.
     * An incompatible value yields {@code null}. A non-blank string that is not a date or time throws.
     *
     * @param raw        source value, never {@code null}
     * @param targetType date or time type to coerce toward
     * @param <T>        target type
     * @return coerced value, or {@code null} when {@code raw} does not match {@code targetType}
     * @throws TypeCoercionException when a non-blank string is not a date or time
     */
    @SuppressWarnings("unchecked")
    public static <T> T coerce(Object raw, Class<T> targetType) {
        if (targetType == java.sql.Date.class) {
            if (raw instanceof java.sql.Date d) return (T) d;
            Long millis = epochMillis(raw);
            if (millis != null) {
                return (T) new java.sql.Date(millis);
            }
            if (raw instanceof String s) {
                LocalDate parsed = requireLocalDate(s, targetType);
                return parsed == null ? null : (T) java.sql.Date.valueOf(parsed);
            }
            return null;
        }

        if (targetType == Time.class) {
            if (raw instanceof Time t) return (T) t;
            Long millis = epochMillis(raw);
            if (millis != null) {
                return (T) new Time(millis);
            }
            if (raw instanceof String s) {
                LocalTime parsed = requireLocalTime(s, targetType);
                return parsed == null ? null : (T) Time.valueOf(parsed);
            }
            return null;
        }

        if (targetType == Timestamp.class) {
            if (raw instanceof Timestamp ts) return (T) ts;
            Long millis = epochMillis(raw);
            if (millis != null) {
                return (T) new Timestamp(millis);
            }
            if (raw instanceof String s) {
                LocalDateTime parsed = requireLocalDateTime(s, targetType);
                return parsed == null ? null : (T) Timestamp.valueOf(parsed);
            }
            return null;
        }

        if (targetType == Date.class) {
            if (raw instanceof Timestamp ts) return (T) new Date(ts.getTime());
            if (raw instanceof java.sql.Date d) return (T) new Date(d.getTime());
            if (raw instanceof Date d) return (T) d;
            if (raw instanceof LocalDateTime ldt) return (T) DateTimeUtil.toDate(ldt);
            if (raw instanceof LocalDate ld) return (T) DateTimeUtil.toDate(ld);
            if (raw instanceof Instant ins) return (T) DateTimeUtil.toDate(ins);
            if (raw instanceof String s) {
                LocalDateTime parsed = requireLocalDateTime(s, targetType);
                return parsed == null ? null : (T) DateTimeUtil.toDate(parsed);
            }
            return null;
        }

        if (targetType == LocalDate.class) {
            if (raw instanceof LocalDate ld) return (T) ld;
            if (raw instanceof java.sql.Date d) return (T) DateTimeUtil.toLocalDate(d);
            if (raw instanceof Timestamp ts) return (T) DateTimeUtil.toLocalDate(ts);
            if (raw instanceof Date d) return (T) DateTimeUtil.toLocalDate(d);
            if (raw instanceof Instant ins) return (T) DateTimeUtil.toLocalDate(ins);
            if (raw instanceof ZonedDateTime zdt) return (T) DateTimeUtil.toLocalDate(zdt);
            if (raw instanceof OffsetDateTime odt) return (T) DateTimeUtil.toLocalDate(odt);
            if (raw instanceof String s) {
                return (T) requireLocalDate(s, targetType);
            }
            return null;
        }

        if (targetType == LocalTime.class) {
            if (raw instanceof LocalTime lt) return (T) lt;
            if (raw instanceof LocalDateTime ldt) return (T) DateTimeUtil.toLocalTime(ldt);
            if (raw instanceof Time t) return (T) DateTimeUtil.toLocalTime(t);
            if (raw instanceof Timestamp ts) return (T) DateTimeUtil.toLocalTime(ts);
            if (raw instanceof Date d) return (T) DateTimeUtil.toLocalTime(d);
            if (raw instanceof Instant ins) return (T) DateTimeUtil.toLocalTime(DateTimeUtil.toDate(ins));
            if (raw instanceof ZonedDateTime zdt) return (T) DateTimeUtil.toLocalTime(DateTimeUtil.toDate(zdt));
            if (raw instanceof OffsetDateTime odt) return (T) DateTimeUtil.toLocalTime(DateTimeUtil.toDate(odt));
            if (raw instanceof String s) {
                return (T) requireLocalTime(s, targetType);
            }
            return null;
        }

        if (targetType == LocalDateTime.class) {
            if (raw instanceof LocalDateTime ldt) return (T) ldt;
            if (raw instanceof LocalDate ld) return (T) DateTimeUtil.toLocalDateTime(ld);
            if (raw instanceof Timestamp ts) return (T) DateTimeUtil.toLocalDateTime(ts);
            if (raw instanceof Date d) return (T) DateTimeUtil.toLocalDateTime(d);
            if (raw instanceof Instant ins) return (T) DateTimeUtil.toLocalDateTime(ins);
            if (raw instanceof ZonedDateTime zdt) return (T) DateTimeUtil.toLocalDateTime(zdt);
            if (raw instanceof OffsetDateTime odt) return (T) DateTimeUtil.toLocalDateTime(odt);
            if (raw instanceof String s) {
                return (T) requireLocalDateTime(s, targetType);
            }
            return null;
        }

        if (targetType == Instant.class) {
            if (raw instanceof Instant ins) return (T) ins;
            if (raw instanceof ZonedDateTime zdt) return (T) zdt.toInstant();
            if (raw instanceof OffsetDateTime odt) return (T) odt.toInstant();
            if (raw instanceof LocalDateTime ldt) return (T) DateTimeUtil.toZonedDateTime(ldt).toInstant();
            if (raw instanceof LocalDate ld) return (T) DateTimeUtil.toZonedDateTime(ld).toInstant();
            if (raw instanceof Timestamp ts) return (T) ts.toInstant();
            if (raw instanceof Date d) return (T) d.toInstant();
            if (raw instanceof String s) {
                LocalDateTime parsed = requireLocalDateTime(s, targetType);
                return parsed == null ? null : (T) DateTimeUtil.toZonedDateTime(parsed).toInstant();
            }
            return null;
        }

        if (targetType == ZonedDateTime.class) {
            if (raw instanceof ZonedDateTime zdt) return (T) zdt;
            if (raw instanceof OffsetDateTime odt) return (T) odt.toZonedDateTime();
            if (raw instanceof Instant ins) return (T) DateTimeUtil.toZonedDateTime(ins);
            if (raw instanceof LocalDateTime ldt) return (T) DateTimeUtil.toZonedDateTime(ldt);
            if (raw instanceof LocalDate ld) return (T) DateTimeUtil.toZonedDateTime(ld);
            if (raw instanceof Timestamp ts) return (T) DateTimeUtil.toZonedDateTime(ts);
            if (raw instanceof Date d) return (T) DateTimeUtil.toZonedDateTime(d);
            if (raw instanceof String s) {
                LocalDateTime parsed = requireLocalDateTime(s, targetType);
                return parsed == null ? null : (T) DateTimeUtil.toZonedDateTime(parsed);
            }
            return null;
        }

        if (targetType == OffsetDateTime.class) {
            if (raw instanceof OffsetDateTime odt) return (T) odt;
            if (raw instanceof Instant ins) return (T) DateTimeUtil.toOffsetDateTime(ins);
            if (raw instanceof ZonedDateTime zdt) return (T) DateTimeUtil.toOffsetDateTime(zdt);
            if (raw instanceof LocalDateTime ldt) return (T) DateTimeUtil.toOffsetDateTime(ldt);
            if (raw instanceof LocalDate ld) return (T) DateTimeUtil.toOffsetDateTime(DateTimeUtil.toLocalDateTime(ld));
            if (raw instanceof Timestamp ts) return (T) DateTimeUtil.toOffsetDateTime(ts);
            if (raw instanceof Date d) return (T) DateTimeUtil.toOffsetDateTime(d);
            if (raw instanceof String s) {
                LocalDateTime parsed = requireLocalDateTime(s, targetType);
                return parsed == null ? null : (T) DateTimeUtil.toOffsetDateTime(parsed);
            }
            return null;
        }

        if (targetType == Year.class) {
            if (raw instanceof Year year) return (T) year;
            if (raw instanceof Number number) {
                try {
                    return (T) Year.of(NumberUtils.toIntExact(number));
                } catch (ArithmeticException | DateTimeException e) {
                    throw cannotCoerce(targetType);
                }
            }
            LocalDate date = localDateOf(raw);
            if (date != null) return (T) Year.from(date);
            if (raw instanceof String text) {
                LocalDate parsed = requireLocalDate(text, targetType);
                return parsed == null ? null : (T) Year.from(parsed);
            }
            return null;
        }

        if (targetType == YearMonth.class) {
            if (raw instanceof YearMonth yearMonth) return (T) yearMonth;
            LocalDate date = localDateOf(raw);
            if (date != null) return (T) YearMonth.from(date);
            if (raw instanceof String text) {
                LocalDate parsed = requireLocalDate(text, targetType);
                return parsed == null ? null : (T) YearMonth.from(parsed);
            }
            return null;
        }

        if (targetType == MonthDay.class) {
            if (raw instanceof MonthDay monthDay) return (T) monthDay;
            LocalDate date = localDateOf(raw);
            if (date != null) return (T) MonthDay.from(date);
            if (raw instanceof String text) {
                LocalDate parsed = requireLocalDate(text, targetType);
                return parsed == null ? null : (T) MonthDay.from(parsed);
            }
            return null;
        }

        if (targetType == OffsetTime.class) {
            if (raw instanceof OffsetTime offsetTime) return (T) offsetTime;
            if (raw instanceof OffsetDateTime offsetDateTime) return (T) offsetDateTime.toOffsetTime();
            if (raw instanceof ZonedDateTime zonedDateTime) return (T) zonedDateTime.toOffsetDateTime().toOffsetTime();
            if (raw instanceof String text) {
                return (T) requirePresent(DateTimeUtil.tryParseOffsetTime(text), text, targetType);
            }
            return null;
        }

        if (targetType == ZoneId.class) {
            if (raw instanceof ZoneId zoneId) return (T) zoneId;
            if (raw instanceof ZonedDateTime zonedDateTime) return (T) zonedDateTime.getZone();
            if (raw instanceof OffsetDateTime offsetDateTime) return (T) offsetDateTime.getOffset();
            if (raw instanceof String text) {
                return (T) requirePresent(DateTimeUtil.tryParseZoneId(text), text, targetType);
            }
            return null;
        }

        if (targetType == ZoneOffset.class) {
            if (raw instanceof ZoneOffset zoneOffset) return (T) zoneOffset;
            if (raw instanceof OffsetDateTime offsetDateTime) return (T) offsetDateTime.getOffset();
            if (raw instanceof ZonedDateTime zonedDateTime) return (T) zonedDateTime.getOffset();
            if (raw instanceof ZoneId) return null;
            if (raw instanceof String text) {
                return (T) requirePresent(DateTimeUtil.tryParseZoneOffset(text), text, targetType);
            }
            return null;
        }
        throw cannotCoerce(targetType);
    }

    /**
     * Calendar date carried by {@code raw}, using the system default zone for an instant,
     * a legacy {@link Date}, and a {@link Calendar}.
     */
    private static LocalDate localDateOf(Object raw) {
        if (raw instanceof LocalDate localDate) return localDate;
        if (raw instanceof LocalDateTime localDateTime) return localDateTime.toLocalDate();
        if (raw instanceof ZonedDateTime zonedDateTime) return zonedDateTime.toLocalDate();
        if (raw instanceof OffsetDateTime offsetDateTime) return offsetDateTime.toLocalDate();
        if (raw instanceof java.sql.Date sqlDate) return sqlDate.toLocalDate();
        if (raw instanceof Instant instant) return DateTimeUtil.toLocalDate(instant);
        if (raw instanceof Calendar calendar) return DateTimeUtil.toLocalDate(calendar);
        if (raw instanceof Date date) return DateTimeUtil.toLocalDate(date);
        return null;
    }

    private static <T> T requirePresent(T parsed, String text, Class<?> targetType) {
        if (parsed == null && !text.isBlank()) {
            throw cannotCoerce(targetType);
        }
        return parsed;
    }

    /**
     * @return parsed date, or {@code null} when {@code text} is blank
     * @throws TypeCoercionException when {@code text} holds something that is not a date
     */
    private static LocalDate requireLocalDate(String text, Class<?> targetType) {
        LocalDate parsed = DateTimeUtil.tryParseLocalDate(text);
        if (parsed == null && !text.isBlank()) {
            throw cannotCoerce(targetType);
        }
        return parsed;
    }

    /**
     * @return parsed date-time, or {@code null} when {@code text} is blank
     * @throws TypeCoercionException when {@code text} holds something that is not a date-time
     */
    private static LocalDateTime requireLocalDateTime(String text, Class<?> targetType) {
        LocalDateTime parsed = DateTimeUtil.tryParseLocalDateTime(text);
        if (parsed == null && !text.isBlank()) {
            throw cannotCoerce(targetType);
        }
        return parsed;
    }

    /**
     * @return parsed time, or {@code null} when {@code text} is blank
     * @throws TypeCoercionException when {@code text} holds something that is not a time
     */
    private static LocalTime requireLocalTime(String text, Class<?> targetType) {
        LocalTime parsed = DateTimeUtil.tryParseLocalTime(text);
        if (parsed == null && !text.isBlank()) {
            throw cannotCoerce(targetType);
        }
        return parsed;
    }

    private static Long epochMillis(Object value) {
        if (value instanceof Date date) return date.getTime();
        if (value instanceof Calendar calendar) return calendar.getTimeInMillis();
        if (value instanceof Number number) return number.longValue();
        if (value instanceof LocalDateTime localDateTime) return Timestamp.valueOf(localDateTime).getTime();
        if (value instanceof LocalDate localDate) return java.sql.Date.valueOf(localDate).getTime();
        if (value instanceof LocalTime localTime) return Time.valueOf(localTime).getTime();
        if (value instanceof OffsetDateTime offsetDateTime) return offsetDateTime.toInstant().toEpochMilli();
        if (value instanceof ZonedDateTime zonedDateTime) return zonedDateTime.toInstant().toEpochMilli();
        if (value instanceof Instant instant) return instant.toEpochMilli();
        if (value instanceof String text && StringUtils.isDigits(text)) return Long.parseLong(text);
        return null;
    }

    private static TypeCoercionException cannotCoerce(Class<?> targetType) {
        String message = "Cannot coerce value to " + targetType.getName();
        return new TypeCoercionException(message);
    }
}
