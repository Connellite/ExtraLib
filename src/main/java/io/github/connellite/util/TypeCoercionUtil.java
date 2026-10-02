package io.github.connellite.util;

import io.github.connellite.exception.TypeCoercionException;
import io.github.connellite.jdbc.LobUtils;
import io.github.connellite.reflection.ReflectionUtil;
import io.github.connellite.reflection.SimpleMapBeanMapper;
import io.github.connellite.util.internal.ArrayCoercion;
import io.github.connellite.util.internal.DateTimeCoercion;
import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
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
 *       {@link UUID}, enums (by name or ordinal)</li>
 *   <li>arrays of those scalars, including primitive arrays and the matching wrapper arrays
 *       ({@code byte[]}, {@code short[]}, {@code int[]}, {@code long[]}, {@code float[]},
 *       {@code double[]}, {@code char[]}, {@code boolean[]})</li>
 *   <li>JDBC: {@link Blob}, {@link Clob}, {@link java.sql.Date}, {@link java.sql.Time},
 *       {@link java.sql.Timestamp}</li>
 *   <li>legacy and {@code java.time}: {@link java.util.Date}, {@link java.time.LocalDate},
 *       {@link java.time.LocalTime}, {@link java.time.LocalDateTime}, {@link java.time.Instant},
 *       {@link java.time.ZonedDateTime}, {@link java.time.OffsetDateTime}, {@link java.time.Year},
 *       {@link java.time.YearMonth}, {@link java.time.MonthDay}, {@link java.time.OffsetTime},
 *       {@link java.time.ZoneId}, {@link java.time.ZoneOffset}</li>
 * </ul>
 * <p>
 * JDBC date/time targets also accept {@code java.time} values, {@link java.util.Calendar},
 * epoch {@link Number}, and all-digit epoch-millis strings. Other strings delegate to
 * {@link DateTimeUtil} ({@code LocalDate}, {@code LocalTime}, {@code LocalDateTime} formats).
 * </p>
 * <p>
 * A primitive array and the array of its wrapper are copied through {@link NumberUtils}.
 * Any other array is converted element by element. {@code null} elements stay {@code null}
 * in a wrapper array. A {@code null} element stored into a primitive array throws
 * {@link TypeCoercionException}. {@link Blob} still converts to {@code byte[]}.
 * A {@link String} converts to {@code byte[]}/{@link Byte}{@code []} as UTF-8
 * and to {@code char[]}/{@link Character}{@code []} as its characters.
 * The reverse uses {@code new String}: chars as written, bytes and {@link Blob} decoded as UTF-8.
 * {@code char[]}/{@link Character}{@code []} become a {@link Clob} the same way a string does.
 * {@link Byte}{@code []} is accepted wherever {@code byte[]} is: {@link Blob} and {@link UUID}.
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
            if (raw instanceof char[] chars) return (T) new String(chars);
            if (raw instanceof Character[] chars) return (T) new String(NumberUtils.objectCharactersToChars(chars));
            if (raw instanceof byte[] bytes) return (T) new String(bytes, StandardCharsets.UTF_8);
            if (raw instanceof Byte[] bytes) return (T) new String(NumberUtils.objectBytesToBytes(bytes), StandardCharsets.UTF_8);
            if (raw instanceof Blob blob) return (T) coerceBlobToString(blob);
            if (raw.getClass().isArray()) return (T) StringUtils.toString(raw);
            return (T) Objects.toString(raw, null);
        }

        if (targetType.isArray()) {
            return ArrayCoercion.coerce(raw, targetType);
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
                Object value = raw instanceof Byte[] bytes ? NumberUtils.objectBytesToBytes(bytes) : raw;
                return (T) UuidUtil.convert2Uuid(value);
            } catch (IllegalArgumentException e) {
                throw cannotCoerce(boxed, e);
            }
        }

        if (boxed.isEnum()) {
            Class<? extends Enum> enumClass = (Class<? extends Enum>) boxed;
            return (T) coerceEnum(raw, enumClass);
        }

        if (DateTimeCoercion.supports(targetType)) {
            return DateTimeCoercion.coerce(raw, targetType);
        }

        if (boxed == Clob.class) {
            if (raw instanceof Clob clob) return (T) clob;
            if (raw instanceof String s) return (T) coerceStringToClob(s);
            if (raw instanceof char[] chars) return (T) coerceStringToClob(new String(chars));
            if (raw instanceof Character[] chars) return (T) coerceStringToClob(new String(NumberUtils.objectCharactersToChars(chars)));
            return null;
        }

        if (boxed == Blob.class) {
            if (raw instanceof Blob blob) return (T) blob;
            if (raw instanceof byte[] bytes) return (T) coerceByteArrayToBlob(bytes);
            if (raw instanceof Byte[] bytes) return (T) coerceByteArrayToBlob(NumberUtils.objectBytesToBytes(bytes));
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

    private static String coerceBlobToString(Blob blob) {
        try {
            return new String(LobUtils.convertBlobToByteArray(blob), StandardCharsets.UTF_8);
        } catch (SQLException e) {
            throw cannotCoerce(String.class, e);
        }
    }

    private static String coerceClobToString(Clob clob) {
        try {
            return LobUtils.convertClobToString(clob);
        } catch (SQLException e) {
            throw cannotCoerce(String.class, e);
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
