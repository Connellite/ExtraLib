package io.github.connellite.util;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.Function;

@UtilityClass
public class NumberUtils {
    private static final double DEFAULT_DOUBLE_EPSILON = 1e-9d;
    private static final float DEFAULT_FLOAT_EPSILON = 1e-6f;

    /**
     * Converts the given string to a {@link Byte}.
     *
     * @param value the string to convert
     * @return the parsed value, or {@code null} if the input is {@code null}, blank, or invalid
     */
    public static Byte toByte(String value) {
        return parse(value, Byte::valueOf, null);
    }

    /**
     * Converts the given string to a {@link Byte}, or returns {@code fallback} when parsing fails.
     *
     * @param value    the string to convert
     * @param fallback value used when the input is {@code null}, blank, or invalid
     * @return the parsed value, or {@code fallback}
     */
    public static Byte toByte(String value, Byte fallback) {
        return parse(value, Byte::valueOf, fallback);
    }

    /**
     * Converts the given string to a {@link Short}.
     *
     * @param value the string to convert
     * @return the parsed value, or {@code null} if the input is {@code null}, blank, or invalid
     */
    public static Short toShort(String value) {
        return parse(value, Short::valueOf, null);
    }

    /**
     * Converts the given string to a {@link Short}, or returns {@code fallback} when parsing fails.
     *
     * @param value    the string to convert
     * @param fallback value used when the input is {@code null}, blank, or invalid
     * @return the parsed value, or {@code fallback}
     */
    public static Short toShort(String value, Short fallback) {
        return parse(value, Short::valueOf, fallback);
    }

    /**
     * Converts the given string to an {@link Integer}.
     *
     * @param value the string to convert
     * @return the parsed value, or {@code null} if the input is {@code null}, blank, or invalid
     */
    public static Integer toInteger(String value) {
        return parse(value, Integer::valueOf, null);
    }

    /**
     * Converts the given string to an {@link Integer}, or returns {@code fallback} when parsing fails.
     *
     * @param value    the string to convert
     * @param fallback value used when the input is {@code null}, blank, or invalid
     * @return the parsed value, or {@code fallback}
     */
    public static Integer toInteger(String value, Integer fallback) {
        return parse(value, Integer::valueOf, fallback);
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
     * Recognizes {@code true}/{@code false}, {@code yes}/{@code no}, {@code on}/{@code off},
     * {@code y}/{@code n}, {@code t}/{@code f} (case-insensitive) and {@code 1}/{@code 0} after trimming.
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
            case "true", "yes", "on", "y", "t", "1" -> true;
            case "false", "no", "off", "n", "f", "0" -> false;
            default -> null;
        };
    }

    /**
     * Parses a string into a {@link Boolean}, or returns {@code fallback} when parsing fails.
     *
     * @param value    the string to convert
     * @param fallback value used when the input is {@code null}, blank, or not a supported literal
     * @return the parsed value, or {@code fallback}
     */
    public static Boolean toBoolean(String value, Boolean fallback) {
        Boolean parsed = toBoolean(value);
        return parsed != null ? parsed : fallback;
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
     * C-style conversion from {@code long} to boolean: {@code 0} is {@code false}, any non-zero value is {@code true}.
     *
     * @param value long value
     * @return {@code false} if {@code value == 0}, otherwise {@code true}
     */
    public static boolean toBoolean(long value) {
        return value != 0L;
    }

    /**
     * C-style conversion from {@code float} to boolean: {@code 0.0f} and {@code -0.0f} are {@code false},
     * any other value (including {@code NaN} and infinities) is {@code true}.
     *
     * @param value float value
     * @return {@code false} if value is zero, otherwise {@code true}
     */
    public static boolean toBoolean(float value) {
        return value != 0.0f;
    }

    /**
     * C-style conversion from {@code double} to boolean: {@code 0.0d} and {@code -0.0d} are {@code false},
     * any other value (including {@code NaN} and infinities) is {@code true}.
     *
     * @param value double value
     * @return {@code false} if value is zero, otherwise {@code true}
     */
    public static boolean toBoolean(double value) {
        return value != 0.0d;
    }

    /**
     * Converts the given string to a {@link Long}.
     *
     * @param value the string to convert
     * @return the parsed value, or {@code null} if the input is {@code null}, blank, or invalid
     */
    public static Long toLong(String value) {
        return parse(value, Long::valueOf, null);
    }

    /**
     * Converts the given string to a {@link Long}, or returns {@code fallback} when parsing fails.
     *
     * @param value    the string to convert
     * @param fallback value used when the input is {@code null}, blank, or invalid
     * @return the parsed value, or {@code fallback}
     */
    public static Long toLong(String value, Long fallback) {
        return parse(value, Long::valueOf, fallback);
    }

    /**
     * Converts the given string to a {@link Float}.
     *
     * @param value the string to convert
     * @return the parsed value, or {@code null} if the input is {@code null}, blank, or invalid
     */
    public static Float toFloat(String value) {
        return parse(value, Float::valueOf, null);
    }

    /**
     * Converts the given string to a {@link Float}, or returns {@code fallback} when parsing fails.
     *
     * @param value    the string to convert
     * @param fallback value used when the input is {@code null}, blank, or invalid
     * @return the parsed value, or {@code fallback}
     */
    public static Float toFloat(String value, Float fallback) {
        return parse(value, Float::valueOf, fallback);
    }

    /**
     * Converts the given string to a {@link Double}.
     *
     * @param value the string to convert
     * @return the parsed value, or {@code null} if the input is {@code null}, blank, or invalid
     */
    public static Double toDouble(String value) {
        return parse(value, Double::valueOf, null);
    }

    /**
     * Converts the given string to a {@link Double}, or returns {@code fallback} when parsing fails.
     *
     * @param value    the string to convert
     * @param fallback value used when the input is {@code null}, blank, or invalid
     * @return the parsed value, or {@code fallback}
     */
    public static Double toDouble(String value, Double fallback) {
        return parse(value, Double::valueOf, fallback);
    }

    /**
     * Converts the given string to a {@link BigInteger}.
     *
     * @param value the string to convert
     * @return the parsed value, or {@code null} if the input is {@code null}, blank, or invalid
     */
    public static BigInteger toBigInteger(String value) {
        return parse(value, BigInteger::new, null);
    }

    /**
     * Converts the given string to a {@link BigInteger}, or returns {@code fallback} when parsing fails.
     *
     * @param value    the string to convert
     * @param fallback value used when the input is {@code null}, blank, or invalid
     * @return the parsed value, or {@code fallback}
     */
    public static BigInteger toBigInteger(String value, BigInteger fallback) {
        return parse(value, BigInteger::new, fallback);
    }

    /**
     * Converts the given string to a {@link BigDecimal}.
     *
     * @param value the string to convert
     * @return the parsed value, or {@code null} if the input is {@code null}, blank, or invalid
     */
    public static BigDecimal toBigDecimal(String value) {
        return parse(value, NumberUtils::parseBigDecimal, null);
    }

    /**
     * Converts the given string to a {@link BigDecimal}, or returns {@code fallback} when parsing fails.
     *
     * @param value    the string to convert
     * @param fallback value used when the input is {@code null}, blank, or invalid
     * @return the parsed value, or {@code fallback}
     */
    public static BigDecimal toBigDecimal(String value, BigDecimal fallback) {
        return parse(value, NumberUtils::parseBigDecimal, fallback);
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
     * Parses the given text into the requested numeric wrapper type, or returns {@code fallback}
     * when parsing fails or the target type is not supported.
     *
     * @param text        the string to parse
     * @param targetClass the target numeric type
     * @param fallback    value used when the input is {@code null}, blank, invalid, or unsupported
     * @param <T>         the numeric type
     * @return the parsed value, or {@code fallback}
     */
    public static <T extends Number> T parseNumber(String text, @NonNull Class<T> targetClass, T fallback) {
        T parsed = parseNumber(text, targetClass);
        return parsed != null ? parsed : fallback;
    }

    /**
     * Narrows {@code value} to the requested numeric wrapper type without silent truncation.
     * <p>
     * Integral targets ({@link Byte}, {@link Short}, {@link Integer}, {@link Long}, {@link BigInteger})
     * reject fractional values and out-of-range magnitudes with {@link ArithmeticException}.
     * {@link Float} and {@link Double} use the usual widening conversions.
     *
     * @param value       the source value, must not be {@code null}
     * @param targetClass the target numeric wrapper type, must not be {@code null}
     * @return the narrowed value
     * @throws ArithmeticException if an exact integral conversion is not possible
     */
    public static Number narrowNumber(@NonNull Number value, @NonNull Class<?> targetClass) {
        if (targetClass == Byte.class) return toByteExact(value);
        if (targetClass == Short.class) return toShortExact(value);
        if (targetClass == Integer.class) return toIntExact(value);
        if (targetClass == Long.class) return toLongExact(value);
        if (targetClass == Float.class) return value.floatValue();
        if (targetClass == Double.class) return value.doubleValue();
        if (targetClass == BigDecimal.class) return toBigDecimal(value);
        if (targetClass == BigInteger.class) return toBigIntegerExact(value);
        return value;
    }

    /**
     * Converts {@code value} to {@code byte}, rejecting fractional parts and out-of-range magnitudes.
     *
     * @param value the source value, must not be {@code null}
     * @return the exact {@code byte} value
     * @throws ArithmeticException if the value has a fractional part or does not fit in {@code byte}
     */
    public static byte toByteExact(@NonNull Number value) {
        return (byte) narrowToRange(value, Byte.MIN_VALUE, Byte.MAX_VALUE);
    }

    /**
     * Converts {@code value} to {@code short}, rejecting fractional parts and out-of-range magnitudes.
     *
     * @param value the source value, must not be {@code null}
     * @return the exact {@code short} value
     * @throws ArithmeticException if the value has a fractional part or does not fit in {@code short}
     */
    public static short toShortExact(@NonNull Number value) {
        return (short) narrowToRange(value, Short.MIN_VALUE, Short.MAX_VALUE);
    }

    /**
     * Converts {@code value} to {@code int}, rejecting fractional parts and out-of-range magnitudes.
     *
     * @param value the source value, must not be {@code null}
     * @return the exact {@code int} value
     * @throws ArithmeticException if the value has a fractional part or does not fit in {@code int}
     */
    public static int toIntExact(@NonNull Number value) {
        return (int) narrowToRange(value, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Converts {@code value} to {@code long}, rejecting fractional parts and out-of-range magnitudes.
     *
     * @param value the source value, must not be {@code null}
     * @return the exact {@code long} value
     * @throws ArithmeticException if the value has a fractional part or does not fit in {@code long}
     */
    public static long toLongExact(@NonNull Number value) {
        return exactLongValue(value);
    }

    /**
     * Converts {@code value} to {@link BigInteger}, rejecting fractional parts.
     *
     * @param value the source value, must not be {@code null}
     * @return the exact {@link BigInteger} value
     * @throws ArithmeticException if the value has a fractional part
     */
    public static BigInteger toBigIntegerExact(@NonNull Number value) {
        if (value instanceof BigInteger bi) return bi;
        if (value instanceof BigDecimal bd) return bd.toBigIntegerExact();
        if (value instanceof Float || value instanceof Double) {
            return new BigDecimal(value.toString()).toBigIntegerExact();
        }
        return BigInteger.valueOf(value.longValue());
    }

    /**
     * Converts {@code value} to {@link BigDecimal}.
     *
     * @param value the source value, must not be {@code null}
     * @return the {@link BigDecimal} representation
     */
    public static BigDecimal toBigDecimal(@NonNull Number value) {
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof BigInteger bi) return new BigDecimal(bi);
        return new BigDecimal(value.toString());
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
     * @throws NullPointerException if any element of {@code bytes} is {@code null}
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
     * Copies a primitive {@code short[]} into a boxed {@link Short}{@code []} of the same length.
     * Each element is autoboxed; the returned array is a new instance.
     *
     * @param shorts the source array, or {@code null}
     * @return a new {@code Short[]} with the same values and order, or {@code null} if {@code shorts} is {@code null}
     */
    public static Short[] shortsToObjectShorts(short[] shorts) {
        if (shorts == null) return null;
        Short[] result = new Short[shorts.length];

        for (int i = 0; i < shorts.length; ++i) {
            result[i] = shorts[i];
        }

        return result;
    }

    /**
     * Copies a boxed {@link Short}{@code []} into a primitive {@code short[]} of the same length.
     * Each element is unboxed; the returned array is a new instance.
     *
     * @param shorts the source array, or {@code null}
     * @return a new {@code short[]} with the same values and order, or {@code null} if {@code shorts} is {@code null}
     * @throws NullPointerException if any element of {@code shorts} is {@code null}
     */
    public static short[] objectShortsToShorts(Short[] shorts) {
        if (shorts == null) return null;
        short[] result = new short[shorts.length];

        for (int i = 0; i < shorts.length; ++i) {
            result[i] = shorts[i];
        }

        return result;
    }

    /**
     * Copies a primitive {@code int[]} into a boxed {@link Integer}{@code []} of the same length.
     * Each element is autoboxed; the returned array is a new instance.
     *
     * @param ints the source array, or {@code null}
     * @return a new {@code Integer[]} with the same values and order, or {@code null} if {@code ints} is {@code null}
     */
    public static Integer[] intsToObjectIntegers(int[] ints) {
        if (ints == null) return null;
        Integer[] result = new Integer[ints.length];

        for (int i = 0; i < ints.length; ++i) {
            result[i] = ints[i];
        }

        return result;
    }

    /**
     * Copies a boxed {@link Integer}{@code []} into a primitive {@code int[]} of the same length.
     * Each element is unboxed; the returned array is a new instance.
     *
     * @param ints the source array, or {@code null}
     * @return a new {@code int[]} with the same values and order, or {@code null} if {@code ints} is {@code null}
     * @throws NullPointerException if any element of {@code ints} is {@code null}
     */
    public static int[] objectIntegersToInts(Integer[] ints) {
        if (ints == null) return null;
        int[] result = new int[ints.length];

        for (int i = 0; i < ints.length; ++i) {
            result[i] = ints[i];
        }

        return result;
    }

    /**
     * Copies a primitive {@code long[]} into a boxed {@link Long}{@code []} of the same length.
     * Each element is autoboxed; the returned array is a new instance.
     *
     * @param longs the source array, or {@code null}
     * @return a new {@code Long[]} with the same values and order, or {@code null} if {@code longs} is {@code null}
     */
    public static Long[] longsToObjectLongs(long[] longs) {
        if (longs == null) return null;
        Long[] result = new Long[longs.length];

        for (int i = 0; i < longs.length; ++i) {
            result[i] = longs[i];
        }

        return result;
    }

    /**
     * Copies a boxed {@link Long}{@code []} into a primitive {@code long[]} of the same length.
     * Each element is unboxed; the returned array is a new instance.
     *
     * @param longs the source array, or {@code null}
     * @return a new {@code long[]} with the same values and order, or {@code null} if {@code longs} is {@code null}
     * @throws NullPointerException if any element of {@code longs} is {@code null}
     */
    public static long[] objectLongsToLongs(Long[] longs) {
        if (longs == null) return null;
        long[] result = new long[longs.length];

        for (int i = 0; i < longs.length; ++i) {
            result[i] = longs[i];
        }

        return result;
    }

    /**
     * Copies a primitive {@code float[]} into a boxed {@link Float}{@code []} of the same length.
     * Each element is autoboxed; the returned array is a new instance.
     *
     * @param floats the source array, or {@code null}
     * @return a new {@code Float[]} with the same values and order, or {@code null} if {@code floats} is {@code null}
     */
    public static Float[] floatsToObjectFloats(float[] floats) {
        if (floats == null) return null;
        Float[] result = new Float[floats.length];

        for (int i = 0; i < floats.length; ++i) {
            result[i] = floats[i];
        }

        return result;
    }

    /**
     * Copies a boxed {@link Float}{@code []} into a primitive {@code float[]} of the same length.
     * Each element is unboxed; the returned array is a new instance.
     *
     * @param floats the source array, or {@code null}
     * @return a new {@code float[]} with the same values and order, or {@code null} if {@code floats} is {@code null}
     * @throws NullPointerException if any element of {@code floats} is {@code null}
     */
    public static float[] objectFloatsToFloats(Float[] floats) {
        if (floats == null) return null;
        float[] result = new float[floats.length];

        for (int i = 0; i < floats.length; ++i) {
            result[i] = floats[i];
        }

        return result;
    }

    /**
     * Copies a primitive {@code double[]} into a boxed {@link Double}{@code []} of the same length.
     * Each element is autoboxed; the returned array is a new instance.
     *
     * @param doubles the source array, or {@code null}
     * @return a new {@code Double[]} with the same values and order, or {@code null} if {@code doubles} is {@code null}
     */
    public static Double[] doublesToObjectDoubles(double[] doubles) {
        if (doubles == null) return null;
        Double[] result = new Double[doubles.length];

        for (int i = 0; i < doubles.length; ++i) {
            result[i] = doubles[i];
        }

        return result;
    }

    /**
     * Copies a boxed {@link Double}{@code []} into a primitive {@code double[]} of the same length.
     * Each element is unboxed; the returned array is a new instance.
     *
     * @param doubles the source array, or {@code null}
     * @return a new {@code double[]} with the same values and order, or {@code null} if {@code doubles} is {@code null}
     * @throws NullPointerException if any element of {@code doubles} is {@code null}
     */
    public static double[] objectDoublesToDoubles(Double[] doubles) {
        if (doubles == null) return null;
        double[] result = new double[doubles.length];

        for (int i = 0; i < doubles.length; ++i) {
            result[i] = doubles[i];
        }

        return result;
    }

    /**
     * Copies a primitive {@code char[]} into a boxed {@link Character}{@code []} of the same length.
     * Each element is autoboxed; the returned array is a new instance.
     *
     * @param chars the source array, or {@code null}
     * @return a new {@code Character[]} with the same values and order, or {@code null} if {@code chars} is {@code null}
     */
    public static Character[] charsToObjectCharacters(char[] chars) {
        if (chars == null) return null;
        Character[] result = new Character[chars.length];

        for (int i = 0; i < chars.length; ++i) {
            result[i] = chars[i];
        }

        return result;
    }

    /**
     * Copies a boxed {@link Character}{@code []} into a primitive {@code char[]} of the same length.
     * Each element is unboxed; the returned array is a new instance.
     *
     * @param chars the source array, or {@code null}
     * @return a new {@code char[]} with the same values and order, or {@code null} if {@code chars} is {@code null}
     * @throws NullPointerException if any element of {@code chars} is {@code null}
     */
    public static char[] objectCharactersToChars(Character[] chars) {
        if (chars == null) return null;
        char[] result = new char[chars.length];

        for (int i = 0; i < chars.length; ++i) {
            result[i] = chars[i];
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

    /**
     * Compares two {@code double} values using a tolerance.
     * <p>
     * Values are considered equal when {@code Math.abs(a - b) <= epsilon}.
     * For special values this method follows intuitive equality:
     * two {@code NaN} values are equal, and infinities are equal only when they have the same sign.
     *
     * @param a       the first value
     * @param b       the second value
     * @param epsilon allowed absolute difference (must be non-negative and finite)
     * @return {@code true} if values are equal within {@code epsilon}
     * @throws IllegalArgumentException if {@code epsilon} is negative, {@code NaN}, or infinite
     */
    public static boolean equals(double a, double b, double epsilon) {
        validateEpsilon(epsilon);
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return Double.isNaN(a) && Double.isNaN(b);
        }
        if (Double.isInfinite(a) || Double.isInfinite(b)) {
            return a == b;
        }
        return Math.abs(a - b) <= epsilon;
    }

    /**
     * Compares two {@code double} values using a default tolerance of {@code 1e-9}.
     *
     * @param a the first value
     * @param b the second value
     * @return {@code true} if values are equal within the default tolerance
     */
    public static boolean equals(double a, double b) {
        return equals(a, b, DEFAULT_DOUBLE_EPSILON);
    }

    /**
     * Compares two {@code float} values using a tolerance.
     * <p>
     * Values are considered equal when {@code Math.abs(a - b) <= epsilon}.
     * For special values this method follows intuitive equality:
     * two {@code NaN} values are equal, and infinities are equal only when they have the same sign.
     *
     * @param a       the first value
     * @param b       the second value
     * @param epsilon allowed absolute difference (must be non-negative and finite)
     * @return {@code true} if values are equal within {@code epsilon}
     * @throws IllegalArgumentException if {@code epsilon} is negative, {@code NaN}, or infinite
     */
    public static boolean equals(float a, float b, float epsilon) {
        validateEpsilon(epsilon);
        if (Float.isNaN(a) || Float.isNaN(b)) {
            return Float.isNaN(a) && Float.isNaN(b);
        }
        if (Float.isInfinite(a) || Float.isInfinite(b)) {
            return a == b;
        }
        return Math.abs(a - b) <= epsilon;
    }

    /**
     * Compares two {@code float} values using a default tolerance of {@code 1e-6}.
     *
     * @param a the first value
     * @param b the second value
     * @return {@code true} if values are equal within the default tolerance
     */
    public static boolean equals(float a, float b) {
        return equals(a, b, DEFAULT_FLOAT_EPSILON);
    }

    /**
     * Returns whether the given {@code double} is effectively zero
     * using the default {@code double} tolerance.
     *
     * @param a the value to test
     * @return {@code true} if {@code a} is equal to {@code 0.0} within the default epsilon
     */
    public static boolean isZero(double a) {
        return equals(a, 0.0);
    }

    /**
     * Returns whether the given {@code double} is effectively zero
     * using a custom tolerance.
     *
     * @param a       the value to test
     * @param epsilon allowed absolute difference from zero (must be non-negative and finite)
     * @return {@code true} if {@code a} is equal to {@code 0.0} within {@code epsilon}
     * @throws IllegalArgumentException if {@code epsilon} is negative, {@code NaN}, or infinite
     */
    public static boolean isZero(double a, double epsilon) {
        return equals(a, 0.0, epsilon);
    }

    /**
     * Returns whether the given {@code float} is effectively zero
     * using the default {@code float} tolerance.
     *
     * @param a the value to test
     * @return {@code true} if {@code a} is equal to {@code 0.0f} within the default epsilon
     */
    public static boolean isZero(float a) {
        return equals(a, 0.0f);
    }

    /**
     * Returns whether the given {@code float} is effectively zero
     * using a custom tolerance.
     *
     * @param a       the value to test
     * @param epsilon allowed absolute difference from zero (must be non-negative and finite)
     * @return {@code true} if {@code a} is equal to {@code 0.0f} within {@code epsilon}
     * @throws IllegalArgumentException if {@code epsilon} is negative, {@code NaN}, or infinite
     */
    public static boolean isZero(float a, float epsilon) {
        return equals(a, 0.0f, epsilon);
    }

    /**
     * Comparator for {@link Double} values with epsilon-based equality.
     * Values whose difference is within epsilon are treated as equal (compare returns {@code 0}).
     */
    public record DoubleComparator(double epsilon) implements Comparator<Double> {
        /**
         * Creates comparator with default epsilon ({@code 1e-9}).
         */
        public DoubleComparator() {
            this(DEFAULT_DOUBLE_EPSILON);
        }

        /**
         * Creates comparator with custom epsilon.
         *
         * @param epsilon allowed absolute difference (must be non-negative and finite)
         */
        public DoubleComparator {
            validateEpsilon(epsilon);
        }

        @Override
        public int compare(Double first, Double second) {
            if (first == null && second == null) {
                return 0;
            }
            if (first == null) {
                return -1;
            }
            if (second == null) {
                return 1;
            }
            return NumberUtils.equals(first, second, epsilon) ? 0 : Double.compare(first, second);
        }
    }

    /**
     * Comparator for {@link Float} values with epsilon-based equality.
     * Values whose difference is within epsilon are treated as equal (compare returns {@code 0}).
     */
    public record FloatComparator(float epsilon) implements Comparator<Float> {
        /**
         * Creates comparator with default epsilon ({@code 1e-6}).
         */
        public FloatComparator() {
            this(DEFAULT_FLOAT_EPSILON);
        }

        /**
         * Creates comparator with custom epsilon.
         *
         * @param epsilon allowed absolute difference (must be non-negative and finite)
         */
        public FloatComparator {
            validateEpsilon(epsilon);
        }

        @Override
        public int compare(Float first, Float second) {
            if (first == null && second == null) {
                return 0;
            }
            if (first == null) {
                return -1;
            }
            if (second == null) {
                return 1;
            }
            return NumberUtils.equals(first, second, epsilon) ? 0 : Float.compare(first, second);
        }
    }

    private static long narrowToRange(Number value, long min, long max) {
        long narrowed = exactLongValue(value);
        if (narrowed < min || narrowed > max) {
            throw new ArithmeticException("integer overflow");
        }
        return narrowed;
    }

    private static long exactLongValue(Number value) {
        if (value instanceof BigDecimal bd) return bd.longValueExact();
        if (value instanceof BigInteger bi) return bi.longValueExact();
        if (value instanceof Float || value instanceof Double) {
            return new BigDecimal(value.toString()).longValueExact();
        }
        return value.longValue();
    }

    private static void validateEpsilon(double epsilon) {
        if (Double.isNaN(epsilon) || Double.isInfinite(epsilon) || epsilon < 0d) {
            throw new IllegalArgumentException("epsilon must be non-negative and finite");
        }
    }

    private static void validateEpsilon(float epsilon) {
        if (Float.isNaN(epsilon) || Float.isInfinite(epsilon) || epsilon < 0f) {
            throw new IllegalArgumentException("epsilon must be non-negative and finite");
        }
    }

    private static <T> T parse(String value, Function<String, T> parser, T fallback) {
        try {
            return value == null || value.isBlank() ? fallback : parser.apply(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static BigDecimal parseBigDecimal(String trimmed) {
        return isHexNumber(trimmed) ? new BigDecimal(decodeBigInteger(trimmed)) : new BigDecimal(trimmed);
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
