package io.github.connellite.reflection.internal;

import io.github.connellite.jdbc.LobUtils;
import io.github.connellite.reflection.ReflectionUtil;
import io.github.connellite.util.DateTimeUtil;
import io.github.connellite.util.NumberUtils;
import io.github.connellite.util.UuidUtil;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@UtilityClass
public class ReflectionTypeCoercionUtil {

    /**
     * Coerces {@code raw} toward {@code fieldType}. {@code label} is only used in error messages
     * (for example a JDBC column label or a map key).
     *
     * @throws Exception coercion failure (for example {@link java.sql.SQLException} from LOB helpers,
     *                    or {@link IllegalArgumentException} for unsupported combinations)
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object coerceDefault(Object raw, Class<?> fieldType, String label) throws Exception {
        if (raw == null) {
            return null;
        }
        Class<?> boxed = ReflectionUtil.primitiveToWrapper(fieldType);

        if (boxed == String.class) {
            if (raw instanceof String s) return s;
            if (raw instanceof Clob clob) return LobUtils.convertClobToString(clob);
            return Objects.toString(raw, null);
        }

        if (boxed == byte[].class) {
            if (raw instanceof byte[] bytes) return bytes;
            if (raw instanceof Blob blob) return LobUtils.convertBlobToByteArray(blob);
            return null;
        }

        if (boxed.isInstance(raw) && !(raw instanceof Number)) {
            return raw;
        }
        if (boxed.isInstance(raw)) {
            return narrowNumber((Number) raw, boxed);
        }

        if (boxed == Boolean.class) {
            if (raw instanceof Boolean b) return b;
            if (raw instanceof Number n) return NumberUtils.toBoolean(n.longValue());
            if (raw instanceof String s) return NumberUtils.toBoolean(s);
            return null;
        }

        if (Number.class.isAssignableFrom(boxed)) {
            if (raw instanceof Number n) return narrowNumber(n, boxed);
            if (raw instanceof String s) {
                Class<? extends Number> numClass = (Class<? extends Number>) boxed;
                return NumberUtils.parseNumber(s, numClass);
            }
            return null;
        }

        if (boxed == Character.class) {
            if (raw instanceof Character c) return c;
            if (raw instanceof Number n) return (char) n.intValue();
            if (raw instanceof String s) return s.isEmpty() ? null : s.charAt(0);
            return null;
        }

        if (boxed == UUID.class) {
            try {
                return UuidUtil.convert2Uuid(raw);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Cannot map label '" + label + "' to UUID", e);
            }
        }

        if (boxed.isEnum()) {
            Class<? extends Enum> enumClass = (Class<? extends Enum>) boxed;
            if (raw instanceof String s) {
                String name = s.trim();
                if (name.isEmpty()) return null;
                try {
                    return Enum.valueOf(enumClass, name);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Cannot map label '" + label + "' to enum " + enumClass.getName(), e);
                }
            }
            if (enumClass.isInstance(raw)) return raw;
            throw new IllegalArgumentException("Cannot map label '" + label + "' to enum " + enumClass.getName());
        }

        if (boxed == java.sql.Date.class) {
            if (raw instanceof java.sql.Date d) return d;
            if (raw instanceof Date d) return new java.sql.Date(d.getTime());
            if (raw instanceof String s) {
                try {
                    return java.sql.Date.valueOf(DateTimeUtil.parseLocalDate(s));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Cannot map label '" + label + "' to java.sql.Date", e);
                }
            }
            return null;
        }

        if (boxed == Clob.class) {
            if (raw instanceof Clob clob) return clob;
            if (raw instanceof String s) return LobUtils.createClob(s);
            return null;
        }

        if (boxed == Blob.class) {
            if (raw instanceof Blob blob) return blob;
            if (raw instanceof byte[] bytes) return LobUtils.createBlob(bytes);
            return null;
        }

        if (boxed == Time.class) {
            if (raw instanceof Time t) return t;
            if (raw instanceof Timestamp ts) return new Time(ts.getTime());
            if (raw instanceof Date d) return new Time(d.getTime());
            if (raw instanceof String s) {
                try {
                    return Time.valueOf(DateTimeUtil.parseLocalTime(s));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Cannot map label '" + label + "' to java.sql.Time", e);
                }
            }
            return null;
        }

        if (boxed == Timestamp.class) {
            if (raw instanceof Timestamp ts) return ts;
            if (raw instanceof Date d) return new Timestamp(d.getTime());
            if (raw instanceof String s) {
                try {
                    return Timestamp.valueOf(DateTimeUtil.parseLocalDateTime(s));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Cannot map label '" + label + "' to java.sql.Timestamp", e);
                }
            }
            return null;
        }

        if (boxed == Date.class) {
            if (raw instanceof Timestamp ts) return new Date(ts.getTime());
            if (raw instanceof java.sql.Date d) return new Date(d.getTime());
            if (raw instanceof Date d) return d;
            if (raw instanceof LocalDateTime ldt) return DateTimeUtil.toDate(ldt);
            if (raw instanceof LocalDate ld) return DateTimeUtil.toDate(ld);
            if (raw instanceof Instant ins) return DateTimeUtil.toDate(ins);
            if (raw instanceof String s) {
                try {
                    return DateTimeUtil.toDate(DateTimeUtil.parseLocalDateTime(s));
                } catch (IllegalArgumentException e) {
                    try {
                        return DateTimeUtil.toDate(DateTimeUtil.parseLocalDate(s));
                    } catch (IllegalArgumentException e2) {
                        throw new IllegalArgumentException("Cannot map label '" + label + "' to java.util.Date", e2);
                    }
                }
            }
            return null;
        }

        if (boxed == LocalDate.class) {
            if (raw instanceof LocalDate ld) return ld;
            if (raw instanceof java.sql.Date d) return DateTimeUtil.toLocalDate(d);
            if (raw instanceof Timestamp ts) return DateTimeUtil.toLocalDate(ts);
            if (raw instanceof Date d) return DateTimeUtil.toLocalDate(d);
            if (raw instanceof Instant ins) return DateTimeUtil.toLocalDate(ins);
            if (raw instanceof ZonedDateTime zdt) return DateTimeUtil.toLocalDate(zdt);
            if (raw instanceof OffsetDateTime odt) return DateTimeUtil.toLocalDate(odt);
            if (raw instanceof String s) {
                try {
                    return DateTimeUtil.parseLocalDate(s);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Cannot map label '" + label + "' to LocalDate", e);
                }
            }
            return null;
        }

        if (boxed == LocalTime.class) {
            if (raw instanceof LocalTime lt) return lt;
            if (raw instanceof LocalDateTime ldt) return DateTimeUtil.toLocalTime(ldt);
            if (raw instanceof Time t) return DateTimeUtil.toLocalTime(t);
            if (raw instanceof Timestamp ts) return DateTimeUtil.toLocalTime(ts);
            if (raw instanceof Date d) return DateTimeUtil.toLocalTime(d);
            if (raw instanceof Instant ins) return DateTimeUtil.toLocalTime(DateTimeUtil.toDate(ins));
            if (raw instanceof ZonedDateTime zdt) return DateTimeUtil.toLocalTime(DateTimeUtil.toDate(zdt));
            if (raw instanceof OffsetDateTime odt) return DateTimeUtil.toLocalTime(DateTimeUtil.toDate(odt));
            if (raw instanceof String s) {
                try {
                    return DateTimeUtil.parseLocalTime(s);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Cannot map label '" + label + "' to LocalTime", e);
                }
            }
            return null;
        }

        if (boxed == LocalDateTime.class) {
            if (raw instanceof LocalDateTime ldt) return ldt;
            if (raw instanceof LocalDate ld) return DateTimeUtil.toLocalDateTime(ld);
            if (raw instanceof Timestamp ts) return DateTimeUtil.toLocalDateTime(ts);
            if (raw instanceof Date d) return DateTimeUtil.toLocalDateTime(d);
            if (raw instanceof Instant ins) return DateTimeUtil.toLocalDateTime(ins);
            if (raw instanceof ZonedDateTime zdt) return DateTimeUtil.toLocalDateTime(zdt);
            if (raw instanceof OffsetDateTime odt) return DateTimeUtil.toLocalDateTime(odt);
            if (raw instanceof String s) {
                try {
                    return DateTimeUtil.parseLocalDateTime(s);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Cannot map label '" + label + "' to LocalDateTime", e);
                }
            }
            return null;
        }

        if (boxed == Instant.class) {
            if (raw instanceof Instant ins) return ins;
            if (raw instanceof ZonedDateTime zdt) return zdt.toInstant();
            if (raw instanceof OffsetDateTime odt) return odt.toInstant();
            if (raw instanceof LocalDateTime ldt) return DateTimeUtil.toZonedDateTime(ldt).toInstant();
            if (raw instanceof LocalDate ld) return DateTimeUtil.toZonedDateTime(ld).toInstant();
            if (raw instanceof Timestamp ts) return ts.toInstant();
            if (raw instanceof Date d) return d.toInstant();
            if (raw instanceof String s) {
                try {
                    return DateTimeUtil.toZonedDateTime(DateTimeUtil.parseLocalDateTime(s)).toInstant();
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Cannot map label '" + label + "' to Instant", e);
                }
            }
            return null;
        }

        if (boxed == ZonedDateTime.class) {
            if (raw instanceof ZonedDateTime zdt) return zdt;
            if (raw instanceof OffsetDateTime odt) return odt.toZonedDateTime();
            if (raw instanceof Instant ins) return DateTimeUtil.toZonedDateTime(ins);
            if (raw instanceof LocalDateTime ldt) return DateTimeUtil.toZonedDateTime(ldt);
            if (raw instanceof LocalDate ld) return DateTimeUtil.toZonedDateTime(ld);
            if (raw instanceof Timestamp ts) return DateTimeUtil.toZonedDateTime(ts);
            if (raw instanceof Date d) return DateTimeUtil.toZonedDateTime(d);
            if (raw instanceof String s) {
                try {
                    return DateTimeUtil.toZonedDateTime(DateTimeUtil.parseLocalDateTime(s));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Cannot map label '" + label + "' to ZonedDateTime", e);
                }
            }
            return null;
        }

        if (boxed == OffsetDateTime.class) {
            if (raw instanceof OffsetDateTime odt) return odt;
            if (raw instanceof Instant ins) return DateTimeUtil.toOffsetDateTime(ins);
            if (raw instanceof ZonedDateTime zdt) return DateTimeUtil.toOffsetDateTime(zdt);
            if (raw instanceof LocalDateTime ldt) return DateTimeUtil.toOffsetDateTime(ldt);
            if (raw instanceof LocalDate ld) return DateTimeUtil.toOffsetDateTime(DateTimeUtil.toLocalDateTime(ld));
            if (raw instanceof Timestamp ts) return DateTimeUtil.toOffsetDateTime(ts);
            if (raw instanceof Date d) return DateTimeUtil.toOffsetDateTime(d);
            if (raw instanceof String s) {
                try {
                    return DateTimeUtil.toOffsetDateTime(DateTimeUtil.parseLocalDateTime(s));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Cannot map label '" + label + "' to OffsetDateTime", e);
                }
            }
            return null;
        }
        throw new IllegalArgumentException("Unsupported field type " + fieldType.getName() + " for label '" + label + "'");
    }

    private static Number narrowNumber(Number n, Class<?> boxed) {
        if (boxed == Byte.class) return n.byteValue();
        if (boxed == Short.class) return n.shortValue();
        if (boxed == Integer.class) return n.intValue();
        if (boxed == Long.class) return n.longValue();
        if (boxed == Float.class) return n.floatValue();
        if (boxed == Double.class) return n.doubleValue();
        if (boxed == BigDecimal.class) return new BigDecimal(n.toString());
        if (boxed == BigInteger.class) {
            if (n instanceof BigDecimal bd) return bd.toBigInteger();
            return BigInteger.valueOf(n.longValue());
        }
        return n;
    }
}
