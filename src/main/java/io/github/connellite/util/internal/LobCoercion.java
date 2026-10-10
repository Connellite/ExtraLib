package io.github.connellite.util.internal;

import io.github.connellite.exception.TypeCoercionException;
import io.github.connellite.jdbc.LobUtils;
import io.github.connellite.util.NumberUtils;
import io.github.connellite.util.TypeCoercionUtil;
import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.SQLException;

/**
 * JDBC LOB coercion used by {@link TypeCoercionUtil}.
 */
@UtilityClass
public class LobCoercion {

    /**
     * Returns whether {@code targetType} is {@link Blob}, {@link Clob}, or {@link NClob}.
     *
     * @param targetType desired type, never {@code null}
     * @return {@code true} for a LOB target
     */
    public static boolean supports(Class<?> targetType) {
        return targetType == Blob.class || targetType == Clob.class || targetType == NClob.class;
    }

    /**
     * Coerces {@code raw} toward a LOB {@code targetType}.
     * {@link #supports(Class)} is {@code true} for {@code targetType}.
     * An incompatible value yields {@code null}.
     *
     * @param raw        source value, never {@code null}
     * @param targetType LOB type to coerce toward
     * @param <T>        target type
     * @return coerced LOB, or {@code null} when {@code raw} does not match {@code targetType}
     * @throws TypeCoercionException when a matching value cannot be stored in a LOB
     */
    @SuppressWarnings("unchecked")
    public static <T> T coerce(Object raw, Class<T> targetType) {
        if (targetType == Blob.class) {
            if (raw instanceof Blob blob) return (T) blob;
            if (raw instanceof byte[] bytes) return (T) byteArrayToBlob(bytes);
            if (raw instanceof Byte[] bytes) return (T) byteArrayToBlob(NumberUtils.objectBytesToBytes(bytes));
            return null;
        }
        if (targetType == Clob.class || targetType == NClob.class) {
            if (targetType.isInstance(raw)) return (T) raw;
            String text = characterText(raw);
            if (text == null) return null;
            return targetType == NClob.class ? (T) stringToNClob(text) : (T) stringToClob(text);
        }
        throw cannotCoerce(targetType, null);
    }

    /**
     * Full text of {@code clob}, or {@code null} if {@code clob} is {@code null}.
     */
    public static String clobToString(Clob clob) {
        try {
            return LobUtils.convertClobToString(clob);
        } catch (SQLException e) {
            throw cannotCoerce(String.class, e);
        }
    }

    /**
     * UTF-8 text of {@code blob}.
     */
    public static String blobToString(Blob blob) {
        try {
            return new String(LobUtils.convertBlobToByteArray(blob), StandardCharsets.UTF_8);
        } catch (SQLException e) {
            throw cannotCoerce(String.class, e);
        }
    }

    private static String characterText(Object raw) {
        if (raw instanceof String s) return s;
        if (raw instanceof char[] chars) return new String(chars);
        if (raw instanceof Character[] chars) return new String(NumberUtils.objectCharactersToChars(chars));
        return null;
    }

    private static Clob stringToClob(String value) {
        try {
            return LobUtils.createClob(value);
        } catch (SQLException e) {
            throw cannotCoerce(Clob.class, e);
        }
    }

    private static NClob stringToNClob(String value) {
        try {
            return LobUtils.createNClob(value);
        } catch (SQLException e) {
            throw cannotCoerce(NClob.class, e);
        }
    }

    private static Blob byteArrayToBlob(byte[] bytes) {
        try {
            return LobUtils.createBlob(bytes);
        } catch (SQLException e) {
            throw cannotCoerce(Blob.class, e);
        }
    }

    private static TypeCoercionException cannotCoerce(Class<?> targetType, Throwable cause) {
        String message = "Cannot coerce value to " + targetType.getName();
        if (cause == null) {
            return new TypeCoercionException(message);
        }
        return new TypeCoercionException(message, cause);
    }
}
