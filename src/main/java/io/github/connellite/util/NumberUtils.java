package io.github.connellite.util;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;
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
     * Converts {@code true} to {@code 1} and {@code false} to {@code 0} (same mapping as a typical C {@code int} cast).
     *
     * @param value boolean to convert
     * @return {@code 1} or {@code 0}, never {@code null}
     */
    public static int toInteger(boolean value) {
        return value ? 1 : 0;
    }

    /**
     * Parses a string into a {@link Boolean}.
     * Recognizes {@code "true"} / {@code "false"} (case-insensitive) and {@code "1"} / {@code "0"} after trimming.
     *
     * @param value the string to convert
     * @return the parsed value, or {@code null} if the input is {@code null}, blank, or not one of the supported literals
     */
    public static Boolean toBoolean(String value) {
        if (value == null) {
            return null;
        }
        String s = value.trim().toLowerCase();
        return switch (s) {
            case "true", "1" -> true;
            case "false", "0" -> false;
            default -> null;
        };
    }

    /**
     * C-style conversion from {@code int} to boolean: {@code 0} is {@code false}, any non-zero value is {@code true}.
     *
     * @param value Boolean value
     * @return {@code false} if {@code value == 0}, otherwise {@code true}
     */
    public static boolean toBoolean(int value) {
        return value != 0;
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

    /**
     * Copies a primitive {@code byte[]} into a boxed {@link Byte}{@code []} of the same length.
     * Each element is autoboxed; the returned array is a new instance.
     *
     * @param bytes the source array, or {@code null}
     * @return a new {@code Byte[]} with the same values and order, or {@code null} if {@code bytes} is {@code null}
     */
    public static Byte[] bytesToObjectBytes(byte[] bytes) {
        if (bytes == null) return null;
        Byte[] result = new Byte[bytes.length];

        for (int i = 0; i < bytes.length; ++i) {
            result[i] = bytes[i];
        }

        return result;
    }

    /**
     * Copies a boxed {@link Byte}{@code []} into a primitive {@code byte[]} of the same length.
     * Each element is unboxed; the returned array is a new instance.
     *
     * @param bytes the source array, or {@code null}
     * @return a new {@code byte[]} with the same values and order, or {@code null} if {@code bytes} is {@code null}
     */
    public static byte[] objectBytesToBytes(Byte[] bytes) {
        if (bytes == null) return null;
        byte[] result = new byte[bytes.length];

        for (int i = 0; i < bytes.length; ++i) {
            result[i] = bytes[i];
        }

        return result;
    }

    /**
     * Equivalent to {@code isNumeric(str, Locale.ROOT)}; see {@link #isNumeric(String, Locale)} for full rules.
     *
     * @param str the text to check, must not be {@code null}
     * @return {@code true} if a root-locale {@link NumberFormat} parses the entire {@code str}
     * @throws NullPointerException if {@code str} is {@code null}
     */
    public static boolean isNumeric(String str) {
        return isNumeric(str, Locale.ROOT);
    }

    /**
     * Returns whether {@code str} is consumed in full as a number by
     * {@link NumberFormat#getInstance(Locale) NumberFormat.getInstance}{@code (locale)}.
     * The result depends on {@code locale}: decimal separators, grouping symbols, and digit shapes follow that
     * locale's {@code NumberFormat} rules.
     *
     * @param str    the text to check, must not be {@code null}
     * @param locale the locale whose number format to use, must not be {@code null}
     * @return {@code true} if the locale's {@code NumberFormat} parses the entire {@code str}
     * @throws NullPointerException if {@code str} or {@code locale} is {@code null}
     */
    public static boolean isNumeric(String str, Locale locale) {
        NumberFormat formatter = NumberFormat.getInstance(locale);
        ParsePosition pos = new ParsePosition(0);
        formatter.parse(str, pos);
        return str.length() == pos.getIndex();
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
