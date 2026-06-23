package io.github.connellite.util;

import io.github.connellite.exception.TypeCoercionException;
import io.github.connellite.jdbc.LobUtils;
import io.github.connellite.reflection.ReflectionUtil;
import io.github.connellite.reflection.SimpleMapBeanMapper;
import lombok.experimental.UtilityClass;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
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

/**
 * Default value coercion for bean and map mapping.
 * <p>
 * Converts a runtime value toward a target Java type. This is the fallback used by
 * {@link io.github.connellite.jdbc.SimpleResultSetBeanMapper} and
 * {@link SimpleMapBeanMapper} when no custom converter is configured.
 * </p>
 * <p>
 * {@code null} input always yields {@code null}. For many target types an incompatible
 * value also yields {@code null} instead of throwing. Parsing failures and unsupported
 * target types throw {@link TypeCoercionException}.
 * </p>
 * <p>
 * Supported targets include:
 * </p>
 * <ul>
 *   <li>scalars: {@link String}, numeric wrappers, {@link Boolean}, {@link Character},
 *       {@code byte[]}, {@link UUID}, enums (by name or ordinal)</li>
 *   <li>JDBC: {@link Blob}, {@link Clob}, {@link java.sql.Date}, {@link Time},
 *       {@link Timestamp}</li>
 *   <li>legacy and {@code java.time}: {@link Date}, {@link LocalDate}, {@link LocalTime},
 *       {@link LocalDateTime}, {@link Instant}, {@link ZonedDateTime},
 *       {@link OffsetDateTime}</li>
 * </ul>
 * <p>
 * String parsing delegates to {@link DateTimeUtil} ({@code LocalDate},
 * {@code LocalTime}, {@code LocalDateTime} formats).
 * </p>
 */
@UtilityClass
public class TypeCoercionUtil {

    /**
     * Coerces {@code raw} toward {@code targetType}.
     *
     * @param raw        source value; {@code null} yields {@code null}
     * @param targetType desired type, including primitives
     * @param <T>        target type
     * @return coerced value compatible with {@code targetType}, or {@code null}
     * @throws TypeCoercionException when coercion fails definitively
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T coerce(Object raw, Class<T> targetType) {
        Objects.requireNonNull(targetType, "targetType");
        if (raw == null) {
            return null;
        }
        Class<?> boxed = ReflectionUtil.primitiveToWrapper(targetType);

        if (boxed == String.class) {
            if (raw instanceof String s) return (T) s;
            if (raw instanceof Clob clob) return (T) coerceClobToString(clob);
            if (raw.getClass().isArray()) return (T) StringUtils.toString(raw);
            return (T) Objects.toString(raw, null);
        }

        if (boxed == byte[].class) {
            if (raw instanceof byte[] bytes) return (T) bytes;
            if (raw instanceof Blob blob) return (T) coerceBlobToByteArray(blob);
            return null;
        }

        if (boxed.isInstance(raw) && !(raw instanceof Number)) {
            return (T) raw;
        }
        if (boxed.isInstance(raw)) {
            return coerceNumber((Number) raw, boxed);
        }

        if (boxed == Boolean.class) {
            if (raw instanceof Boolean b) return (T) b;
            if (raw instanceof Number n) return (T) Boolean.valueOf(NumberUtils.toBoolean(n.longValue()));
            if (raw instanceof String s) return (T) NumberUtils.toBoolean(s);
            return null;
        }

        if (Number.class.isAssignableFrom(boxed)) {
            if (raw instanceof Number n) return coerceNumber(n, boxed);
            if (raw instanceof String s) {
                Class<? extends Number> numClass = (Class<? extends Number>) boxed;
                return (T) NumberUtils.parseNumber(s, numClass);
            }
            return null;
        }

        if (boxed == Character.class) {
            if (raw instanceof Character c) return (T) c;
            if (raw instanceof Number n) return (T) Character.valueOf((char) n.intValue());
            if (raw instanceof String s) return (T) (s.isEmpty() ? null : s.charAt(0));
            return null;
        }

        if (boxed == UUID.class) {
            try {
                return (T) UuidUtil.convert2Uuid(raw);
            } catch (IllegalArgumentException e) {
                throw cannotCoerce(boxed, e);
            }
        }

        if (boxed.isEnum()) {
            Class<? extends Enum> enumClass = (Class<? extends Enum>) boxed;
            return (T) coerceEnum(raw, enumClass);
        }

        if (boxed == java.sql.Date.class) {
            if (raw instanceof java.sql.Date d) return (T) d;
            if (raw instanceof Date d) return (T) new java.sql.Date(d.getTime());
            if (raw instanceof String s) {
                try {
                    return (T) java.sql.Date.valueOf(DateTimeUtil.parseLocalDate(s));
                } catch (IllegalArgumentException e) {
                    throw cannotCoerce(boxed, e);
                }
            }
            return null;
        }

        if (boxed == Clob.class) {
            if (raw instanceof Clob clob) return (T) clob;
            if (raw instanceof String s) return (T) coerceStringToClob(s);
            return null;
        }

        if (boxed == Blob.class) {
            if (raw instanceof Blob blob) return (T) blob;
            if (raw instanceof byte[] bytes) return (T) coerceByteArrayToBlob(bytes);
            return null;
        }

        if (boxed == Time.class) {
            if (raw instanceof Time t) return (T) t;
            if (raw instanceof Timestamp ts) return (T) new Time(ts.getTime());
            if (raw instanceof Date d) return (T) new Time(d.getTime());
            if (raw instanceof String s) {
                try {
                    return (T) Time.valueOf(DateTimeUtil.parseLocalTime(s));
                } catch (IllegalArgumentException e) {
                    throw cannotCoerce(boxed, e);
                }
            }
            return null;
        }

        if (boxed == Timestamp.class) {
            if (raw instanceof Timestamp ts) return (T) ts;
            if (raw instanceof Date d) return (T) new Timestamp(d.getTime());
            if (raw instanceof String s) {
                try {
                    return (T) Timestamp.valueOf(DateTimeUtil.parseLocalDateTime(s));
                } catch (IllegalArgumentException e) {
                    throw cannotCoerce(boxed, e);
                }
            }
            return null;
        }

        if (boxed == Date.class) {
            if (raw instanceof Timestamp ts) return (T) new Date(ts.getTime());
            if (raw instanceof java.sql.Date d) return (T) new Date(d.getTime());
            if (raw instanceof Date d) return (T) d;
            if (raw instanceof LocalDateTime ldt) return (T) DateTimeUtil.toDate(ldt);
            if (raw instanceof LocalDate ld) return (T) DateTimeUtil.toDate(ld);
            if (raw instanceof Instant ins) return (T) DateTimeUtil.toDate(ins);
            if (raw instanceof String s) {
                try {
                    return (T) DateTimeUtil.toDate(DateTimeUtil.parseLocalDateTime(s));
                } catch (IllegalArgumentException e) {
                    try {
                        return (T) DateTimeUtil.toDate(DateTimeUtil.parseLocalDate(s));
                    } catch (IllegalArgumentException e2) {
                        throw cannotCoerce(boxed, e2);
                    }
                }
            }
            return null;
        }

        if (boxed == LocalDate.class) {
            if (raw instanceof LocalDate ld) return (T) ld;
            if (raw instanceof java.sql.Date d) return (T) DateTimeUtil.toLocalDate(d);
            if (raw instanceof Timestamp ts) return (T) DateTimeUtil.toLocalDate(ts);
            if (raw instanceof Date d) return (T) DateTimeUtil.toLocalDate(d);
            if (raw instanceof Instant ins) return (T) DateTimeUtil.toLocalDate(ins);
            if (raw instanceof ZonedDateTime zdt) return (T) DateTimeUtil.toLocalDate(zdt);
            if (raw instanceof OffsetDateTime odt) return (T) DateTimeUtil.toLocalDate(odt);
            if (raw instanceof String s) {
                try {
                    return (T) DateTimeUtil.parseLocalDate(s);
                } catch (IllegalArgumentException e) {
                    throw cannotCoerce(boxed, e);
                }
            }
            return null;
        }

        if (boxed == LocalTime.class) {
            if (raw instanceof LocalTime lt) return (T) lt;
            if (raw instanceof LocalDateTime ldt) return (T) DateTimeUtil.toLocalTime(ldt);
            if (raw instanceof Time t) return (T) DateTimeUtil.toLocalTime(t);
            if (raw instanceof Timestamp ts) return (T) DateTimeUtil.toLocalTime(ts);
            if (raw instanceof Date d) return (T) DateTimeUtil.toLocalTime(d);
            if (raw instanceof Instant ins) return (T) DateTimeUtil.toLocalTime(DateTimeUtil.toDate(ins));
            if (raw instanceof ZonedDateTime zdt) return (T) DateTimeUtil.toLocalTime(DateTimeUtil.toDate(zdt));
            if (raw instanceof OffsetDateTime odt) return (T) DateTimeUtil.toLocalTime(DateTimeUtil.toDate(odt));
            if (raw instanceof String s) {
                try {
                    return (T) DateTimeUtil.parseLocalTime(s);
                } catch (IllegalArgumentException e) {
                    throw cannotCoerce(boxed, e);
                }
            }
            return null;
        }

        if (boxed == LocalDateTime.class) {
            if (raw instanceof LocalDateTime ldt) return (T) ldt;
            if (raw instanceof LocalDate ld) return (T) DateTimeUtil.toLocalDateTime(ld);
            if (raw instanceof Timestamp ts) return (T) DateTimeUtil.toLocalDateTime(ts);
            if (raw instanceof Date d) return (T) DateTimeUtil.toLocalDateTime(d);
            if (raw instanceof Instant ins) return (T) DateTimeUtil.toLocalDateTime(ins);
            if (raw instanceof ZonedDateTime zdt) return (T) DateTimeUtil.toLocalDateTime(zdt);
            if (raw instanceof OffsetDateTime odt) return (T) DateTimeUtil.toLocalDateTime(odt);
            if (raw instanceof String s) {
                try {
                    return (T) DateTimeUtil.parseLocalDateTime(s);
                } catch (IllegalArgumentException e) {
                    throw cannotCoerce(boxed, e);
                }
            }
            return null;
        }

        if (boxed == Instant.class) {
            if (raw instanceof Instant ins) return (T) ins;
            if (raw instanceof ZonedDateTime zdt) return (T) zdt.toInstant();
            if (raw instanceof OffsetDateTime odt) return (T) odt.toInstant();
            if (raw instanceof LocalDateTime ldt) return (T) DateTimeUtil.toZonedDateTime(ldt).toInstant();
            if (raw instanceof LocalDate ld) return (T) DateTimeUtil.toZonedDateTime(ld).toInstant();
            if (raw instanceof Timestamp ts) return (T) ts.toInstant();
            if (raw instanceof Date d) return (T) d.toInstant();
            if (raw instanceof String s) {
                try {
                    return (T) DateTimeUtil.toZonedDateTime(DateTimeUtil.parseLocalDateTime(s)).toInstant();
                } catch (IllegalArgumentException e) {
                    throw cannotCoerce(boxed, e);
                }
            }
            return null;
        }

        if (boxed == ZonedDateTime.class) {
            if (raw instanceof ZonedDateTime zdt) return (T) zdt;
            if (raw instanceof OffsetDateTime odt) return (T) odt.toZonedDateTime();
            if (raw instanceof Instant ins) return (T) DateTimeUtil.toZonedDateTime(ins);
            if (raw instanceof LocalDateTime ldt) return (T) DateTimeUtil.toZonedDateTime(ldt);
            if (raw instanceof LocalDate ld) return (T) DateTimeUtil.toZonedDateTime(ld);
            if (raw instanceof Timestamp ts) return (T) DateTimeUtil.toZonedDateTime(ts);
            if (raw instanceof Date d) return (T) DateTimeUtil.toZonedDateTime(d);
            if (raw instanceof String s) {
                try {
                    return (T) DateTimeUtil.toZonedDateTime(DateTimeUtil.parseLocalDateTime(s));
                } catch (IllegalArgumentException e) {
                    throw cannotCoerce(boxed, e);
                }
            }
            return null;
        }

        if (boxed == OffsetDateTime.class) {
            if (raw instanceof OffsetDateTime odt) return (T) odt;
            if (raw instanceof Instant ins) return (T) DateTimeUtil.toOffsetDateTime(ins);
            if (raw instanceof ZonedDateTime zdt) return (T) DateTimeUtil.toOffsetDateTime(zdt);
            if (raw instanceof LocalDateTime ldt) return (T) DateTimeUtil.toOffsetDateTime(ldt);
            if (raw instanceof LocalDate ld) return (T) DateTimeUtil.toOffsetDateTime(DateTimeUtil.toLocalDateTime(ld));
            if (raw instanceof Timestamp ts) return (T) DateTimeUtil.toOffsetDateTime(ts);
            if (raw instanceof Date d) return (T) DateTimeUtil.toOffsetDateTime(d);
            if (raw instanceof String s) {
                try {
                    return (T) DateTimeUtil.toOffsetDateTime(DateTimeUtil.parseLocalDateTime(s));
                } catch (IllegalArgumentException e) {
                    throw cannotCoerce(boxed, e);
                }
            }
            return null;
        }
        throw unsupportedTarget(targetType);
    }

    @SuppressWarnings("unchecked")
    private static <T> T coerceNumber(Number value, Class<?> targetType) {
        try {
            return (T) NumberUtils.narrowNumber(value, targetType);
        } catch (ArithmeticException | IllegalArgumentException e) {
            throw cannotCoerce(targetType, e);
        }
    }

    private static String coerceClobToString(Clob clob) {
        try {
            return LobUtils.convertClobToString(clob);
        } catch (SQLException e) {
            throw cannotCoerce(String.class, e);
        }
    }

    private static byte[] coerceBlobToByteArray(Blob blob) {
        try {
            return LobUtils.convertBlobToByteArray(blob);
        } catch (SQLException e) {
            throw cannotCoerce(byte[].class, e);
        }
    }

    private static Clob coerceStringToClob(String value) {
        try {
            return LobUtils.createClob(value);
        } catch (SQLException e) {
            throw cannotCoerce(Clob.class, e);
        }
    }

    private static Blob coerceByteArrayToBlob(byte[] bytes) {
        try {
            return LobUtils.createBlob(bytes);
        } catch (SQLException e) {
            throw cannotCoerce(Blob.class, e);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Enum<?> coerceEnum(Object raw, Class<? extends Enum> enumClass) {
        if (raw instanceof String s) {
            String name = s.trim();
            if (name.isEmpty()) return null;
            try {
                return Enum.valueOf(enumClass, name);
            } catch (IllegalArgumentException e) {
                throw cannotCoerce(enumClass, e);
            }
        }
        if (raw instanceof Number n) {
            int ordinal = n.intValue();
            Enum<?>[] constants = enumClass.getEnumConstants();
            if (ordinal >= 0 && ordinal < constants.length) {
                return constants[ordinal];
            }
            throw cannotCoerce(enumClass, null);
        }
        if (enumClass.isInstance(raw)) return (Enum<?>) raw;
        throw cannotCoerce(enumClass, null);
    }

    private static TypeCoercionException cannotCoerce(Class<?> targetType, Throwable cause) {
        String message = "Cannot coerce value to " + targetType.getName();
        if (cause == null) {
            return new TypeCoercionException(message);
        }
        return new TypeCoercionException(message, cause);
    }

    private static TypeCoercionException unsupportedTarget(Class<?> targetType) {
        return new TypeCoercionException("Unsupported target type: " + targetType.getName());
    }
}
