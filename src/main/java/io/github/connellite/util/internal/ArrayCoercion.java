package io.github.connellite.util.internal;

import io.github.connellite.exception.TypeCoercionException;
import io.github.connellite.jdbc.LobUtils;
import io.github.connellite.util.NumberUtils;
import io.github.connellite.util.TypeCoercionUtil;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.SQLException;

/**
 * Array coercion used by {@link TypeCoercionUtil}.
 */
@UtilityClass
public class ArrayCoercion {

    /**
     * Coerces {@code raw} toward the array type {@code targetType}.
     * The same runtime class is returned as-is. A primitive array and the array of its wrapper
     * are copied through {@link NumberUtils}. Any other array is converted element by element.
     * A non-array source yields {@code null}, except a {@link Blob} when the target is {@code byte[]}
     * and a {@link String} when the target is a {@code byte} or {@code char} array, primitive or boxed.
     *
     * @param raw        source value, never {@code null}
     * @param targetType array type to coerce toward
     * @param <T>        target type
     * @return coerced array, or {@code null} when {@code raw} cannot be stored in {@code targetType}
     * @throws TypeCoercionException when an element cannot be stored in the target array
     */
    @SuppressWarnings("unchecked")
    public static <T> T coerce(Object raw, Class<T> targetType) {
        if (raw.getClass() == targetType) {
            return (T) raw;
        }
        if (targetType == byte[].class && raw instanceof Blob blob) {
            return (T) coerceBlobToByteArray(blob);
        }
        if (raw instanceof String text) {
            Object converted = stringToByteOrCharArray(text, targetType);
            if (converted != null) {
                return (T) converted;
            }
        }
        if (!raw.getClass().isArray()) {
            return null;
        }

        Class<?> sourceComponent = raw.getClass().getComponentType();
        Class<?> targetComponent = targetType.getComponentType();
        try {
            Object copied = copyPrimitiveWrapperArray(raw, sourceComponent, targetComponent);
            if (copied != null) {
                return (T) copied;
            }
        } catch (NullPointerException e) {
            throw cannotCoerce(targetType, e);
        }
        return (T) coerceArrayElements(raw, targetType);
    }

    /**
     * Copies between a primitive array and the array of its wrapper.
     *
     * @return the copied array, or {@code null} when the component types are not such a pair
     * @throws NullPointerException when unboxing a {@code null} element
     */
    private static Object copyPrimitiveWrapperArray(Object raw, Class<?> sourceComponent, Class<?> targetComponent) {
        if (sourceComponent == byte.class && targetComponent == Byte.class) {
            return NumberUtils.bytesToObjectBytes((byte[]) raw);
        }
        if (sourceComponent == Byte.class && targetComponent == byte.class) {
            return NumberUtils.objectBytesToBytes((Byte[]) raw);
        }
        if (sourceComponent == short.class && targetComponent == Short.class) {
            return NumberUtils.shortsToObjectShorts((short[]) raw);
        }
        if (sourceComponent == Short.class && targetComponent == short.class) {
            return NumberUtils.objectShortsToShorts((Short[]) raw);
        }
        if (sourceComponent == int.class && targetComponent == Integer.class) {
            return NumberUtils.intsToObjectIntegers((int[]) raw);
        }
        if (sourceComponent == Integer.class && targetComponent == int.class) {
            return NumberUtils.objectIntegersToInts((Integer[]) raw);
        }
        if (sourceComponent == long.class && targetComponent == Long.class) {
            return NumberUtils.longsToObjectLongs((long[]) raw);
        }
        if (sourceComponent == Long.class && targetComponent == long.class) {
            return NumberUtils.objectLongsToLongs((Long[]) raw);
        }
        if (sourceComponent == float.class && targetComponent == Float.class) {
            return NumberUtils.floatsToObjectFloats((float[]) raw);
        }
        if (sourceComponent == Float.class && targetComponent == float.class) {
            return NumberUtils.objectFloatsToFloats((Float[]) raw);
        }
        if (sourceComponent == double.class && targetComponent == Double.class) {
            return NumberUtils.doublesToObjectDoubles((double[]) raw);
        }
        if (sourceComponent == Double.class && targetComponent == double.class) {
            return NumberUtils.objectDoublesToDoubles((Double[]) raw);
        }
        if (sourceComponent == char.class && targetComponent == Character.class) {
            return NumberUtils.charsToObjectCharacters((char[]) raw);
        }
        if (sourceComponent == Character.class && targetComponent == char.class) {
            return NumberUtils.objectCharactersToChars((Character[]) raw);
        }
        return null;
    }

    /**
     * Converts {@code value} to a {@code byte} or {@code char} array.
     * Bytes are the UTF-8 encoding. Chars are the string's UTF-16 code units.
     *
     * @return the array, or {@code null} when {@code targetType} is not a {@code byte} or {@code char} array
     */
    private static Object stringToByteOrCharArray(String value, Class<?> targetType) {
        if (targetType == char[].class) {
            return value.toCharArray();
        }
        if (targetType == Character[].class) {
            return NumberUtils.charsToObjectCharacters(value.toCharArray());
        }
        if (targetType == byte[].class) {
            return value.getBytes(StandardCharsets.UTF_8);
        }
        if (targetType == Byte[].class) {
            return NumberUtils.bytesToObjectBytes(value.getBytes(StandardCharsets.UTF_8));
        }
        return null;
    }

    private static Object coerceArrayElements(Object raw, Class<?> targetType) {
        Class<?> componentType = targetType.getComponentType();
        int length = Array.getLength(raw);
        Object result = Array.newInstance(componentType, length);
        for (int i = 0; i < length; ++i) {
            Object coerced = TypeCoercionUtil.coerce(Array.get(raw, i), componentType);
            if (coerced == null && componentType.isPrimitive()) {
                throw cannotCoerce(targetType, null);
            }
            Array.set(result, i, coerced);
        }
        return result;
    }

    private static byte[] coerceBlobToByteArray(Blob blob) {
        try {
            return LobUtils.convertBlobToByteArray(blob);
        } catch (SQLException e) {
            throw cannotCoerce(byte[].class, e);
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
