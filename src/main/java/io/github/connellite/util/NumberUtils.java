package io.github.connellite.util;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Function;

@UtilityClass
public class NumberUtils {

    /**
     * Converts the given string to a {@link Byte}.
     *
     * @param value the string to convert
     * @return the parsed value, or {@code null} if the input is {@code null}, blank, or invalid
     */
    public static Byte toByte(String value) {
        return parse(value, Byte::valueOf);
    }

    /**
     * Converts the given string to a {@link Short}.
     *
     * @param value the string to convert
     * @return the parsed value, or {@code null} if the input is {@code null}, blank, or invalid
     */
    public static Short toShort(String value) {
        return parse(value, Short::valueOf);
    }

    /**
     * Converts the given string to an {@link Integer}.
     *
     * @param value the string to convert
     * @return the parsed value, or {@code null} if the input is {@code null}, blank, or invalid
     */
    public static Integer toInteger(String value) {
        return parse(value, Integer::valueOf);
    }

    /**
     * Converts the given string to a {@link Long}.
     *
     * @param value the string to convert
     * @return the parsed value, or {@code null} if the input is {@code null}, blank, or invalid
     */
    public static Long toLong(String value) {
        return parse(value, Long::valueOf);
    }

    /**
     * Converts the given string to a {@link Float}.
     *
     * @param value the string to convert
     * @return the parsed value, or {@code null} if the input is {@code null}, blank, or invalid
     */
    public static Float toFloat(String value) {
        return parse(value, Float::valueOf);
    }

    /**
     * Converts the given string to a {@link Double}.
     *
     * @param value the string to convert
     * @return the parsed value, or {@code null} if the input is {@code null}, blank, or invalid
     */
    public static Double toDouble(String value) {
        return parse(value, Double::valueOf);
    }

    /**
     * Converts the given string to a {@link BigInteger}.
     *
     * @param value the string to convert
     * @return the parsed value, or {@code null} if the input is {@code null}, blank, or invalid
     */
    public static BigInteger toBigInteger(String value) {
        return parse(value, BigInteger::new);
    }

    /**
     * Converts the given string to a {@link BigDecimal}.
     *
     * @param value the string to convert
     * @return the parsed value, or {@code null} if the input is {@code null}, blank, or invalid
     */
    public static BigDecimal toBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return isHexNumber(trimmed) ? new BigDecimal(decodeBigInteger(trimmed)) : new BigDecimal(trimmed);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Parses the given text into the requested numeric wrapper type.
     * <p>
     * Supported target classes are:
     * {@link Byte}, {@link Short}, {@link Integer}, {@link Long},
     * {@link Float}, {@link Double}, {@link BigInteger}, {@link BigDecimal}.
     *
     * @param text        the string to parse
     * @param targetClass the target numeric type
     * @param <T>         the numeric type
     * @return the parsed value, or {@code null} if the input is {@code null}, blank, invalid,
     * or the target type is not supported
     */
    @SuppressWarnings("unchecked")
    public static <T extends Number> T parseNumber(String text, @NonNull Class<T> targetClass) {
        if (Byte.class == targetClass) {
            return (T) toByte(text);
        }
        if (Short.class == targetClass) {
            return (T) toShort(text);
        }
        if (Integer.class == targetClass) {
            return (T) toInteger(text);
        }
        if (Long.class == targetClass) {
            return (T) toLong(text);
        }
        if (Float.class == targetClass) {
            return (T) toFloat(text);
        }
        if (Double.class == targetClass) {
            return (T) toDouble(text);
        }
        if (BigInteger.class == targetClass) {
            return (T) toBigInteger(text);
        }
        if (BigDecimal.class == targetClass) {
            return (T) toBigDecimal(text);
        }
        return null;
    }

    private static <T> T parse(String value, Function<String, T> parser) {
        try {
            return value == null || value.isBlank() ? null : parser.apply(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Determine whether the given {@code value} String indicates a hex number,
     * i.e. needs to be passed into {@code Integer.decode} instead of
     * {@code Integer.valueOf}, etc.
     */
    private static boolean isHexNumber(String value) {
        int index = (value.startsWith("-") ? 1 : 0);
        return (value.startsWith("0x", index) || value.startsWith("0X", index) || value.startsWith("#", index));
    }

    /**
     * Decode a {@link BigInteger} from the supplied {@link String} value.
     * <p>Supports decimal, hex, and octal notation.
     *
     * @see BigInteger#BigInteger(String, int)
     */
    private static BigInteger decodeBigInteger(String value) {
        int radix = 10;
        int index = 0;
        boolean negative = false;

        // Handle minus sign, if present.
        if (value.startsWith("-")) {
            negative = true;
            index++;
        }

        // Handle radix specifier, if present.
        if (value.startsWith("0x", index) || value.startsWith("0X", index)) {
            index += 2;
            radix = 16;
        } else if (value.startsWith("#", index)) {
            index++;
            radix = 16;
        } else if (value.startsWith("0", index) && value.length() > 1 + index) {
            index++;
            radix = 8;
        }

        BigInteger result = new BigInteger(value.substring(index), radix);
        return (negative ? result.negate() : result);
    }
}
