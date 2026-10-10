package io.github.connellite.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NumberUtilsTest {

    @Test
    void toInteger_fromBoolean() {
        assertEquals(1, NumberUtils.toInteger(true));
        assertEquals(0, NumberUtils.toInteger(false));
    }

    @Test
    void toBoolean_fromString() {
        assertEquals(Boolean.TRUE, NumberUtils.toBoolean("true"));
        assertEquals(Boolean.TRUE, NumberUtils.toBoolean("TRUE"));
        assertEquals(Boolean.TRUE, NumberUtils.toBoolean(" 1 "));
        assertEquals(Boolean.TRUE, NumberUtils.toBoolean("yes"));
        assertEquals(Boolean.TRUE, NumberUtils.toBoolean("ON"));
        assertEquals(Boolean.TRUE, NumberUtils.toBoolean("Y"));
        assertEquals(Boolean.TRUE, NumberUtils.toBoolean("t"));
        assertEquals(Boolean.FALSE, NumberUtils.toBoolean("false"));
        assertEquals(Boolean.FALSE, NumberUtils.toBoolean("0"));
        assertEquals(Boolean.FALSE, NumberUtils.toBoolean("no"));
        assertEquals(Boolean.FALSE, NumberUtils.toBoolean("OFF"));
        assertEquals(Boolean.FALSE, NumberUtils.toBoolean("N"));
        assertEquals(Boolean.FALSE, NumberUtils.toBoolean("f"));
    }

    @Test
    void toBoolean_fromString_nullBlankInvalid() {
        assertNull(NumberUtils.toBoolean(null));
        assertNull(NumberUtils.toBoolean(" "));
        assertNull(NumberUtils.toBoolean("maybe"));
    }

    @Test
    void toStringConverters_useFallbackWhenInvalid() {
        assertEquals((byte) 7, NumberUtils.toByte("nope", (byte) 7));
        assertEquals((byte) 3, NumberUtils.toByte("3", (byte) 7));
        assertEquals((short) 7, NumberUtils.toShort(null, (short) 7));
        assertEquals(7, NumberUtils.toInteger(" ", 7));
        assertEquals(42, NumberUtils.toInteger("42", 7));
        assertEquals(Boolean.TRUE, NumberUtils.toBoolean("maybe", Boolean.TRUE));
        assertEquals(Boolean.FALSE, NumberUtils.toBoolean("0", Boolean.TRUE));
        assertEquals(9L, NumberUtils.toLong("x", 9L));
        assertEquals(1.5f, NumberUtils.toFloat("bad", 1.5f));
        assertEquals(2.5d, NumberUtils.toDouble("", 2.5d));
        assertEquals(BigInteger.TEN, NumberUtils.toBigInteger("zz", BigInteger.TEN));
        assertEquals(BigDecimal.ONE, NumberUtils.toBigDecimal("n/a", BigDecimal.ONE));
        assertEquals(new BigDecimal("1.25"), NumberUtils.toBigDecimal("1.25", BigDecimal.ONE));
    }

    @Test
    void toBoolean_fromInt() {
        assertFalse(NumberUtils.toBoolean(0));
        assertTrue(NumberUtils.toBoolean(1));
        assertTrue(NumberUtils.toBoolean(-1));
        assertTrue(NumberUtils.toBoolean(Integer.MAX_VALUE));
    }

    @Test
    void toBoolean_fromLong() {
        assertFalse(NumberUtils.toBoolean(0L));
        assertTrue(NumberUtils.toBoolean(1L));
        assertTrue(NumberUtils.toBoolean(-1L));
        assertTrue(NumberUtils.toBoolean(Long.MAX_VALUE));
    }

    @Test
    void toBoolean_fromFloat() {
        assertFalse(NumberUtils.toBoolean(0.0f));
        assertFalse(NumberUtils.toBoolean(-0.0f));
        assertTrue(NumberUtils.toBoolean(0.1f));
        assertTrue(NumberUtils.toBoolean(-0.1f));
        assertTrue(NumberUtils.toBoolean(Float.NaN));
        assertTrue(NumberUtils.toBoolean(Float.POSITIVE_INFINITY));
    }

    @Test
    void toBoolean_fromDouble() {
        assertFalse(NumberUtils.toBoolean(0.0d));
        assertFalse(NumberUtils.toBoolean(-0.0d));
        assertTrue(NumberUtils.toBoolean(0.1d));
        assertTrue(NumberUtils.toBoolean(-0.1d));
        assertTrue(NumberUtils.toBoolean(Double.NaN));
        assertTrue(NumberUtils.toBoolean(Double.NEGATIVE_INFINITY));
    }

    @Test
    void parsesWrapperNumbers() {
        assertEquals(Byte.valueOf((byte) 12), NumberUtils.toByte("12"));
        assertEquals(Short.valueOf((short) 123), NumberUtils.toShort("123"));
        assertEquals(Integer.valueOf(1234), NumberUtils.toInteger("1234"));
        assertEquals(Long.valueOf(12345L), NumberUtils.toLong("12345"));
        assertEquals(Float.valueOf("12.5"), NumberUtils.toFloat("12.5"));
        assertEquals(Double.valueOf("123.5"), NumberUtils.toDouble("123.5"));
        assertEquals(new BigInteger("12345678901234567890"), NumberUtils.toBigInteger("12345678901234567890"));
        assertEquals(new BigDecimal("12345.6789"), NumberUtils.toBigDecimal("12345.6789"));
    }

    @Test
    void nullAndBlankReturnNull() {
        assertNull(NumberUtils.toByte(null));
        assertNull(NumberUtils.toShort(" "));
        assertNull(NumberUtils.toInteger("   "));
        assertNull(NumberUtils.toLong("\t"));
        assertNull(NumberUtils.toFloat("\n"));
        assertNull(NumberUtils.toDouble("  "));
        assertNull(NumberUtils.toBigInteger(""));
        assertNull(NumberUtils.toBigDecimal(" \t "));
    }

    @Test
    void invalidInputReturnsNull() {
        assertNull(NumberUtils.toByte("128"));
        assertNull(NumberUtils.toShort("abc"));
        assertNull(NumberUtils.toInteger("12.3"));
        assertNull(NumberUtils.toLong("1L"));
        assertNull(NumberUtils.toFloat("value"));
        assertNull(NumberUtils.toDouble("value"));
        assertNull(NumberUtils.toBigInteger("12.3"));
        assertNull(NumberUtils.toBigDecimal("value"));
    }

    @Test
    void toBigDecimal_supportsHexNotation() {
        // 0x10 == 16
        assertEquals(new BigDecimal("16"), NumberUtils.toBigDecimal("0x10"));
        // #10 == 16
        assertEquals(new BigDecimal("16"), NumberUtils.toBigDecimal("#10"));
        // negative hex
        assertEquals(new BigDecimal("-16"), NumberUtils.toBigDecimal("-0x10"));
    }

    @Test
    void parseNumber_parsesSupportedTypes() {
        assertEquals(123, NumberUtils.parseNumber("123", Integer.class));
        assertEquals(123L, NumberUtils.parseNumber("123", Long.class));
        assertEquals(new BigInteger("123"), NumberUtils.parseNumber("123", BigInteger.class));
        assertEquals(new BigDecimal("123.45"), NumberUtils.parseNumber("123.45", BigDecimal.class));
        // hex via parseNumber for BigDecimal
        assertEquals(new BigDecimal("16"), NumberUtils.parseNumber("0x10", BigDecimal.class));
        assertEquals(new BigDecimal("16"), NumberUtils.parseNumber("#10", BigDecimal.class));
    }

    @Test
    void parseNumber_returnsNullForInvalidOrUnsupported() {
        assertNull(NumberUtils.parseNumber("not-a-number", Integer.class));
        assertNull(NumberUtils.parseNumber(" ", Long.class));
        assertNull(NumberUtils.parseNumber("1.2", Integer.class));
        assertThrows(NullPointerException.class, () ->  NumberUtils.parseNumber("10", null));
        assertNull(NumberUtils.parseNumber("10", (Class) Object.class));
    }

    @Test
    void parseNumber_usesFallbackWhenInvalid() {
        assertEquals(7, NumberUtils.parseNumber("nope", Integer.class, 7));
        assertEquals(123, NumberUtils.parseNumber("123", Integer.class, 7));
        assertEquals(9L, NumberUtils.parseNumber(" ", Long.class, 9L));
    }

    @Test
    void bytesToObjectBytes_nullReturnsNull() {
        assertNull(NumberUtils.bytesToObjectBytes(null));
    }

    @Test
    void objectBytesToBytes_nullReturnsNull() {
        assertNull(NumberUtils.objectBytesToBytes(null));
    }

    @Test
    void bytesToObjectBytes_emptyArray() {
        assertArrayEquals(new Byte[0], NumberUtils.bytesToObjectBytes(new byte[0]));
    }

    @Test
    void objectBytesToBytes_emptyArray() {
        assertArrayEquals(new byte[0], NumberUtils.objectBytesToBytes(new Byte[0]));
    }

    @Test
    void bytesRoundTrip_viaObjectBytes() {
        byte[] original = {0, 127, -1, -128};
        Byte[] boxed = NumberUtils.bytesToObjectBytes(original);
        assertEquals(Byte.valueOf((byte) 0), boxed[0]);
        assertEquals(Byte.valueOf((byte) 127), boxed[1]);
        assertEquals(Byte.valueOf((byte) -1), boxed[2]);
        assertEquals(Byte.valueOf((byte) -128), boxed[3]);
        assertArrayEquals(original, NumberUtils.objectBytesToBytes(boxed));
    }

    @Test
    void objectBytesToBytes_nullElementThrows() {
        Byte[] withNull = {(byte) 1, null};
        assertThrows(NullPointerException.class, () -> NumberUtils.objectBytesToBytes(withNull));
    }

    @Test
    void shortsToObjectShorts_nullReturnsNull() {
        assertNull(NumberUtils.shortsToObjectShorts(null));
    }

    @Test
    void objectShortsToShorts_nullReturnsNull() {
        assertNull(NumberUtils.objectShortsToShorts(null));
    }

    @Test
    void shortsToObjectShorts_emptyArray() {
        assertArrayEquals(new Short[0], NumberUtils.shortsToObjectShorts(new short[0]));
    }

    @Test
    void objectShortsToShorts_emptyArray() {
        assertArrayEquals(new short[0], NumberUtils.objectShortsToShorts(new Short[0]));
    }

    @Test
    void shortsRoundTrip_viaObjectShorts() {
        short[] original = {0, Short.MAX_VALUE, -1, Short.MIN_VALUE};
        Short[] boxed = NumberUtils.shortsToObjectShorts(original);
        assertEquals(Short.valueOf((short) 0), boxed[0]);
        assertEquals(Short.valueOf(Short.MAX_VALUE), boxed[1]);
        assertEquals(Short.valueOf((short) -1), boxed[2]);
        assertEquals(Short.valueOf(Short.MIN_VALUE), boxed[3]);
        assertArrayEquals(original, NumberUtils.objectShortsToShorts(boxed));
    }

    @Test
    void objectShortsToShorts_nullElementThrows() {
        Short[] withNull = {(short) 1, null};
        assertThrows(NullPointerException.class, () -> NumberUtils.objectShortsToShorts(withNull));
    }

    @Test
    void intsToObjectIntegers_nullReturnsNull() {
        assertNull(NumberUtils.intsToObjectIntegers(null));
    }

    @Test
    void objectIntegersToInts_nullReturnsNull() {
        assertNull(NumberUtils.objectIntegersToInts(null));
    }

    @Test
    void intsToObjectIntegers_emptyArray() {
        assertArrayEquals(new Integer[0], NumberUtils.intsToObjectIntegers(new int[0]));
    }

    @Test
    void objectIntegersToInts_emptyArray() {
        assertArrayEquals(new int[0], NumberUtils.objectIntegersToInts(new Integer[0]));
    }

    @Test
    void intsRoundTrip_viaObjectIntegers() {
        int[] original = {0, Integer.MAX_VALUE, -1, Integer.MIN_VALUE};
        Integer[] boxed = NumberUtils.intsToObjectIntegers(original);
        assertEquals(Integer.valueOf(0), boxed[0]);
        assertEquals(Integer.valueOf(Integer.MAX_VALUE), boxed[1]);
        assertEquals(Integer.valueOf(-1), boxed[2]);
        assertEquals(Integer.valueOf(Integer.MIN_VALUE), boxed[3]);
        assertArrayEquals(original, NumberUtils.objectIntegersToInts(boxed));
    }

    @Test
    void objectIntegersToInts_nullElementThrows() {
        Integer[] withNull = {1, null};
        assertThrows(NullPointerException.class, () -> NumberUtils.objectIntegersToInts(withNull));
    }

    @Test
    void longsToObjectLongs_nullReturnsNull() {
        assertNull(NumberUtils.longsToObjectLongs(null));
    }

    @Test
    void objectLongsToLongs_nullReturnsNull() {
        assertNull(NumberUtils.objectLongsToLongs(null));
    }

    @Test
    void longsToObjectLongs_emptyArray() {
        assertArrayEquals(new Long[0], NumberUtils.longsToObjectLongs(new long[0]));
    }

    @Test
    void objectLongsToLongs_emptyArray() {
        assertArrayEquals(new long[0], NumberUtils.objectLongsToLongs(new Long[0]));
    }

    @Test
    void longsRoundTrip_viaObjectLongs() {
        long[] original = {0L, Long.MAX_VALUE, -1L, Long.MIN_VALUE};
        Long[] boxed = NumberUtils.longsToObjectLongs(original);
        assertEquals(Long.valueOf(0L), boxed[0]);
        assertEquals(Long.valueOf(Long.MAX_VALUE), boxed[1]);
        assertEquals(Long.valueOf(-1L), boxed[2]);
        assertEquals(Long.valueOf(Long.MIN_VALUE), boxed[3]);
        assertArrayEquals(original, NumberUtils.objectLongsToLongs(boxed));
    }

    @Test
    void objectLongsToLongs_nullElementThrows() {
        Long[] withNull = {1L, null};
        assertThrows(NullPointerException.class, () -> NumberUtils.objectLongsToLongs(withNull));
    }

    @Test
    void floatsToObjectFloats_nullReturnsNull() {
        assertNull(NumberUtils.floatsToObjectFloats(null));
    }

    @Test
    void objectFloatsToFloats_nullReturnsNull() {
        assertNull(NumberUtils.objectFloatsToFloats(null));
    }

    @Test
    void floatsToObjectFloats_emptyArray() {
        assertArrayEquals(new Float[0], NumberUtils.floatsToObjectFloats(new float[0]));
    }

    @Test
    void objectFloatsToFloats_emptyArray() {
        assertArrayEquals(new float[0], NumberUtils.objectFloatsToFloats(new Float[0]));
    }

    @Test
    void floatsRoundTrip_viaObjectFloats() {
        float[] original = {0f, Float.MAX_VALUE, -1f, Float.MIN_VALUE, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY};
        Float[] boxed = NumberUtils.floatsToObjectFloats(original);
        assertEquals(Float.valueOf(0f), boxed[0]);
        assertEquals(Float.valueOf(Float.MAX_VALUE), boxed[1]);
        assertEquals(Float.valueOf(-1f), boxed[2]);
        assertEquals(Float.valueOf(Float.MIN_VALUE), boxed[3]);
        assertEquals(Float.valueOf(Float.NaN), boxed[4]);
        assertEquals(Float.valueOf(Float.POSITIVE_INFINITY), boxed[5]);
        assertEquals(Float.valueOf(Float.NEGATIVE_INFINITY), boxed[6]);
        assertArrayEquals(original, NumberUtils.objectFloatsToFloats(boxed));
    }

    @Test
    void objectFloatsToFloats_nullElementThrows() {
        Float[] withNull = {1f, null};
        assertThrows(NullPointerException.class, () -> NumberUtils.objectFloatsToFloats(withNull));
    }

    @Test
    void doublesToObjectDoubles_nullReturnsNull() {
        assertNull(NumberUtils.doublesToObjectDoubles(null));
    }

    @Test
    void objectDoublesToDoubles_nullReturnsNull() {
        assertNull(NumberUtils.objectDoublesToDoubles(null));
    }

    @Test
    void doublesToObjectDoubles_emptyArray() {
        assertArrayEquals(new Double[0], NumberUtils.doublesToObjectDoubles(new double[0]));
    }

    @Test
    void objectDoublesToDoubles_emptyArray() {
        assertArrayEquals(new double[0], NumberUtils.objectDoublesToDoubles(new Double[0]));
    }

    @Test
    void doublesRoundTrip_viaObjectDoubles() {
        double[] original = {0d, Double.MAX_VALUE, -1d, Double.MIN_VALUE, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        Double[] boxed = NumberUtils.doublesToObjectDoubles(original);
        assertEquals(Double.valueOf(0d), boxed[0]);
        assertEquals(Double.valueOf(Double.MAX_VALUE), boxed[1]);
        assertEquals(Double.valueOf(-1d), boxed[2]);
        assertEquals(Double.valueOf(Double.MIN_VALUE), boxed[3]);
        assertEquals(Double.valueOf(Double.NaN), boxed[4]);
        assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), boxed[5]);
        assertEquals(Double.valueOf(Double.NEGATIVE_INFINITY), boxed[6]);
        assertArrayEquals(original, NumberUtils.objectDoublesToDoubles(boxed));
    }

    @Test
    void objectDoublesToDoubles_nullElementThrows() {
        Double[] withNull = {1d, null};
        assertThrows(NullPointerException.class, () -> NumberUtils.objectDoublesToDoubles(withNull));
    }

    @Test
    void charsToObjectCharacters_nullReturnsNull() {
        assertNull(NumberUtils.charsToObjectCharacters(null));
    }

    @Test
    void objectCharactersToChars_nullReturnsNull() {
        assertNull(NumberUtils.objectCharactersToChars(null));
    }

    @Test
    void charsToObjectCharacters_emptyArray() {
        assertArrayEquals(new Character[0], NumberUtils.charsToObjectCharacters(new char[0]));
    }

    @Test
    void objectCharactersToChars_emptyArray() {
        assertArrayEquals(new char[0], NumberUtils.objectCharactersToChars(new Character[0]));
    }

    @Test
    void charsRoundTrip_viaObjectCharacters() {
        char[] original = {'\0', 'A', '\u00FF', Character.MAX_VALUE};
        Character[] boxed = NumberUtils.charsToObjectCharacters(original);
        assertEquals(Character.valueOf('\0'), boxed[0]);
        assertEquals(Character.valueOf('A'), boxed[1]);
        assertEquals(Character.valueOf('\u00FF'), boxed[2]);
        assertEquals(Character.valueOf(Character.MAX_VALUE), boxed[3]);
        assertArrayEquals(original, NumberUtils.objectCharactersToChars(boxed));
    }

    @Test
    void objectCharactersToChars_nullElementThrows() {
        Character[] withNull = {'A', null};
        assertThrows(NullPointerException.class, () -> NumberUtils.objectCharactersToChars(withNull));
    }

    @Test
    void isNumeric_nullThrows() {
        assertThrows(NullPointerException.class, () -> NumberUtils.isNumeric(null));
    }

    @Test
    void isNumeric_twoArg_nullStrThrows() {
        assertThrows(NullPointerException.class, () -> NumberUtils.isNumeric(null, Locale.ROOT));
    }

    @Test
    void isNumeric_twoArg_nullLocaleThrows() {
        assertThrows(NullPointerException.class, () -> NumberUtils.isNumeric("0", null));
    }

    @Test
    void isNumeric_explicitLocales_parseKnownSamples() {
        assertTrue(NumberUtils.isNumeric("123.45", Locale.ROOT));
        assertTrue(NumberUtils.isNumeric("123.45", Locale.US));
        assertTrue(NumberUtils.isNumeric("123.45", Locale.GERMANY));
        assertTrue(NumberUtils.isNumeric("1,234", Locale.ROOT));
        assertFalse(NumberUtils.isNumeric("12.3.4", Locale.ROOT));
    }

    @Test
    void isNumeric_acceptsRootLocaleNumberFormat() {
        assertTrue(NumberUtils.isNumeric(""));
        assertTrue(NumberUtils.isNumeric("0"));
        assertTrue(NumberUtils.isNumeric("42"));
        assertTrue(NumberUtils.isNumeric("-17"));
        assertTrue(NumberUtils.isNumeric("123.45"));
        assertTrue(NumberUtils.isNumeric("3.14"));
        assertTrue(NumberUtils.isNumeric(".5"));
        assertTrue(NumberUtils.isNumeric("1,234"));
        assertTrue(NumberUtils.isNumeric("1,234.56"));
        assertTrue(NumberUtils.isNumeric("123,45"));
    }

    @Test
    void isNumeric_rejectsInvalidOrUnconsumed() {
        assertFalse(NumberUtils.isNumeric(" "));
        assertFalse(NumberUtils.isNumeric("   "));
        assertFalse(NumberUtils.isNumeric("abc"));
        assertFalse(NumberUtils.isNumeric("12a"));
        assertFalse(NumberUtils.isNumeric("12.3.4"));
        assertFalse(NumberUtils.isNumeric(" 42 "));
        assertFalse(NumberUtils.isNumeric("42 "));
        assertFalse(NumberUtils.isNumeric("1e-3"));
    }

    @Test
    void isNumeric_independentOfDefaultLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            assertTrue(NumberUtils.isNumeric("123.45"));
            assertTrue(NumberUtils.isNumeric("1,234"));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void almostEquals_double_withCustomEpsilon() {
        assertTrue(NumberUtils.equals(0.1d + 0.2d, 0.3d, 1e-9d));
        assertFalse(NumberUtils.equals(1.0d, 1.1d, 1e-3d));
    }

    @Test
    void almostEquals_double_withDefaultEpsilon() {
        assertTrue(NumberUtils.equals(1.0000000001d, 1.0000000002d));
        assertFalse(NumberUtils.equals(1.0d, 1.000001d));
    }

    @Test
    void almostEquals_double_specialValues() {
        assertTrue(NumberUtils.equals(Double.NaN, Double.NaN, 0.0d));
        assertFalse(NumberUtils.equals(Double.NaN, 1.0d, 1e-9d));
        assertTrue(NumberUtils.equals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 1e-9d));
        assertFalse(NumberUtils.equals(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 1e-9d));
    }

    @Test
    void almostEquals_double_invalidEpsilonThrows() {
        assertThrows(IllegalArgumentException.class, () -> NumberUtils.equals(1.0d, 1.0d, -1e-9d));
        assertThrows(IllegalArgumentException.class, () -> NumberUtils.equals(1.0d, 1.0d, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> NumberUtils.equals(1.0d, 1.0d, Double.POSITIVE_INFINITY));
    }

    @Test
    void almostEquals_float_withCustomEpsilon() {
        assertTrue(NumberUtils.equals(0.1f + 0.2f, 0.3f, 1e-5f));
        assertFalse(NumberUtils.equals(1.0f, 1.1f, 1e-3f));
    }

    @Test
    void almostEquals_float_withDefaultEpsilon() {
        assertTrue(NumberUtils.equals(1.0000001f, 1.0000002f));
        assertFalse(NumberUtils.equals(1.0f, 1.01f));
    }

    @Test
    void almostEquals_float_specialValues() {
        assertTrue(NumberUtils.equals(Float.NaN, Float.NaN, 0.0f));
        assertFalse(NumberUtils.equals(Float.NaN, 1.0f, 1e-6f));
        assertTrue(NumberUtils.equals(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, 1e-6f));
        assertFalse(NumberUtils.equals(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 1e-6f));
    }

    @Test
    void almostEquals_float_invalidEpsilonThrows() {
        assertThrows(IllegalArgumentException.class, () -> NumberUtils.equals(1.0f, 1.0f, -1e-6f));
        assertThrows(IllegalArgumentException.class, () -> NumberUtils.equals(1.0f, 1.0f, Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> NumberUtils.equals(1.0f, 1.0f, Float.POSITIVE_INFINITY));
    }

    @Test
    void doubleComparator_usesEpsilonEquality() {
        NumberUtils.DoubleComparator comparator = new NumberUtils.DoubleComparator(1e-9d);
        assertEquals(0, comparator.compare(0.1d + 0.2d, 0.3d));
        assertTrue(comparator.compare(2.0d, 1.0d) > 0);
        assertTrue(comparator.compare(1.0d, 2.0d) < 0);
        assertEquals(1e-9d, comparator.epsilon());
    }

    @Test
    void doubleComparator_nullAndInvalidEpsilonHandling() {
        NumberUtils.DoubleComparator comparator = new NumberUtils.DoubleComparator();
        assertEquals(0, comparator.compare((Double) null, null));
        assertTrue(comparator.compare(null, 1.0d) < 0);
        assertTrue(comparator.compare(1.0d, null) > 0);
        assertThrows(IllegalArgumentException.class, () -> new NumberUtils.DoubleComparator(-1e-9d));
    }

    @Test
    void floatComparator_usesEpsilonEquality() {
        NumberUtils.FloatComparator comparator = new NumberUtils.FloatComparator(1e-5f);
        assertEquals(0, comparator.compare(0.1f + 0.2f, 0.3f));
        assertTrue(comparator.compare(2.0f, 1.0f) > 0);
        assertTrue(comparator.compare(1.0f, 2.0f) < 0);
        assertEquals(1e-5f, comparator.epsilon());
    }

    @Test
    void floatComparator_nullAndInvalidEpsilonHandling() {
        NumberUtils.FloatComparator comparator = new NumberUtils.FloatComparator();
        assertEquals(0, comparator.compare((Float) null, null));
        assertTrue(comparator.compare(null, 1.0f) < 0);
        assertTrue(comparator.compare(1.0f, null) > 0);
        assertThrows(IllegalArgumentException.class, () -> new NumberUtils.FloatComparator(Float.NaN));
    }

    @Test
    void toByteExact_convertsInRangeValues() {
        assertEquals((byte) 42, NumberUtils.toByteExact(42));
        assertEquals((byte) 42, NumberUtils.toByteExact(42L));
        assertEquals((byte) -128, NumberUtils.toByteExact(Byte.MIN_VALUE));
        assertEquals((byte) 127, NumberUtils.toByteExact(Byte.MAX_VALUE));
        assertEquals((byte) 10, NumberUtils.toByteExact(new BigDecimal("10")));
        assertEquals((byte) 10, NumberUtils.toByteExact(BigInteger.TEN));
        assertEquals((byte) 42, NumberUtils.toByteExact(42.0d));
        assertEquals((byte) 42, NumberUtils.toByteExact(42.0f));
    }

    @Test
    void toByteExact_rejectsOverflowAndFractions() {
        assertThrows(ArithmeticException.class, () -> NumberUtils.toByteExact(128));
        assertThrows(ArithmeticException.class, () -> NumberUtils.toByteExact(-129));
        assertThrows(ArithmeticException.class, () -> NumberUtils.toByteExact(new BigDecimal("1.5")));
        assertThrows(ArithmeticException.class, () -> NumberUtils.toByteExact(1.5d));
    }

    @Test
    void toShortExact_convertsInRangeValues() {
        assertEquals((short) 1234, NumberUtils.toShortExact(1234));
        assertEquals((short) 1234, NumberUtils.toShortExact(1234L));
        assertEquals(Short.MIN_VALUE, NumberUtils.toShortExact(Short.MIN_VALUE));
        assertEquals(Short.MAX_VALUE, NumberUtils.toShortExact(Short.MAX_VALUE));
        assertEquals((short) 100, NumberUtils.toShortExact(new BigDecimal("100")));
        assertEquals((short) 100, NumberUtils.toShortExact(BigInteger.valueOf(100)));
    }

    @Test
    void toShortExact_rejectsOverflowAndFractions() {
        assertThrows(ArithmeticException.class, () -> NumberUtils.toShortExact(Short.MAX_VALUE + 1));
        assertThrows(ArithmeticException.class, () -> NumberUtils.toShortExact(Short.MIN_VALUE - 1));
        assertThrows(ArithmeticException.class, () -> NumberUtils.toShortExact(new BigDecimal("1.1")));
    }

    @Test
    void toIntExact_convertsInRangeValues() {
        assertEquals(1234, NumberUtils.toIntExact(1234));
        assertEquals(1234, NumberUtils.toIntExact(1234L));
        assertEquals(Integer.MIN_VALUE, NumberUtils.toIntExact(Integer.MIN_VALUE));
        assertEquals(Integer.MAX_VALUE, NumberUtils.toIntExact(Integer.MAX_VALUE));
        assertEquals(42, NumberUtils.toIntExact(new BigDecimal("42")));
        assertEquals(42, NumberUtils.toIntExact(BigInteger.valueOf(42)));
        assertEquals(7, NumberUtils.toIntExact(7.0f));
    }

    @Test
    void toIntExact_rejectsOverflowAndFractions() {
        assertThrows(ArithmeticException.class, () -> NumberUtils.toIntExact((long) Integer.MAX_VALUE + 1L));
        assertThrows(ArithmeticException.class, () -> NumberUtils.toIntExact((long) Integer.MIN_VALUE - 1L));
        assertThrows(ArithmeticException.class, () -> NumberUtils.toIntExact(new BigDecimal("3.14")));
        assertThrows(ArithmeticException.class, () -> NumberUtils.toIntExact(1.9d));
    }

    @Test
    void toLongExact_convertsIntegralValues() {
        assertEquals(12345L, NumberUtils.toLongExact(12345));
        assertEquals(12345L, NumberUtils.toLongExact(12345L));
        assertEquals(Long.MIN_VALUE, NumberUtils.toLongExact(Long.MIN_VALUE));
        assertEquals(Long.MAX_VALUE, NumberUtils.toLongExact(Long.MAX_VALUE));
        assertEquals(99L, NumberUtils.toLongExact(new BigDecimal("99")));
        assertEquals(99L, NumberUtils.toLongExact(BigInteger.valueOf(99)));
        assertEquals(5L, NumberUtils.toLongExact(5.0d));
    }

    @Test
    void toLongExact_rejectsOverflowAndFractions() {
        BigInteger tooLarge = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
        assertThrows(ArithmeticException.class, () -> NumberUtils.toLongExact(tooLarge));
        assertThrows(ArithmeticException.class, () -> NumberUtils.toLongExact(new BigDecimal("1.25")));
        assertThrows(ArithmeticException.class, () -> NumberUtils.toLongExact(2.5d));
    }

    @Test
    void toBigIntegerExact_convertsIntegralValues() {
        BigInteger expected = new BigInteger("12345678901234567890");
        assertEquals(expected, NumberUtils.toBigIntegerExact(expected));
        assertEquals(BigInteger.TEN, NumberUtils.toBigIntegerExact(10));
        assertEquals(BigInteger.TEN, NumberUtils.toBigIntegerExact(10L));
        assertEquals(BigInteger.valueOf(42), NumberUtils.toBigIntegerExact(new BigDecimal("42")));
        assertEquals(BigInteger.valueOf(7), NumberUtils.toBigIntegerExact(7.0d));
    }

    @Test
    void toBigIntegerExact_rejectsFractions() {
        assertThrows(ArithmeticException.class, () -> NumberUtils.toBigIntegerExact(new BigDecimal("3.14")));
        assertThrows(ArithmeticException.class, () -> NumberUtils.toBigIntegerExact(3.14d));
    }

    @Test
    void toBigDecimal_fromNumber() {
        BigDecimal decimal = new BigDecimal("123.45");
        assertEquals(decimal, NumberUtils.toBigDecimal(decimal));
        assertEquals(new BigDecimal("42"), NumberUtils.toBigDecimal(BigInteger.valueOf(42)));
        assertEquals(new BigDecimal("12.5"), NumberUtils.toBigDecimal(12.5d));
        assertEquals(new BigDecimal("7"), NumberUtils.toBigDecimal(7));
    }

    @Test
    void narrowNumber_narrowsToSupportedTypes() {
        assertEquals((byte) 12, NumberUtils.narrowNumber(12, Byte.class));
        assertEquals((short) 123, NumberUtils.narrowNumber(123, Short.class));
        assertEquals(1234, NumberUtils.narrowNumber(1234L, Integer.class));
        assertEquals(12345L, NumberUtils.narrowNumber(12345, Long.class));
        assertEquals(12.5f, NumberUtils.narrowNumber(12.5d, Float.class));
        assertEquals(12.5d, NumberUtils.narrowNumber(12.5f, Double.class));
        assertEquals(new BigDecimal("10.5"), NumberUtils.narrowNumber(new BigDecimal("10.5"), BigDecimal.class));
        assertEquals(BigInteger.TEN, NumberUtils.narrowNumber(BigInteger.TEN, BigInteger.class));
    }

    @Test
    void narrowNumber_rejectsInexactIntegralConversions() {
        assertThrows(ArithmeticException.class, () -> NumberUtils.narrowNumber(300, Byte.class));
        assertThrows(ArithmeticException.class, () -> NumberUtils.narrowNumber(new BigDecimal("1.5"), Integer.class));
        assertThrows(ArithmeticException.class, () -> NumberUtils.narrowNumber(new BigDecimal("1.5"), BigInteger.class));
    }

    @Test
    void narrowNumber_returnsUnchangedValueForUnsupportedTarget() {
        Number custom = new Number() {
            @Override
            public int intValue() {
                return 1;
            }

            @Override
            public long longValue() {
                return 1L;
            }

            @Override
            public float floatValue() {
                return 1f;
            }

            @Override
            public double doubleValue() {
                return 1d;
            }
        };
        assertEquals(custom, NumberUtils.narrowNumber(custom, Number.class));
    }

    @Test
    void exactNumberMethods_nullArgumentsThrow() {
        assertThrows(NullPointerException.class, () -> NumberUtils.narrowNumber(null, Integer.class));
        assertThrows(NullPointerException.class, () -> NumberUtils.narrowNumber(1, null));
        assertThrows(NullPointerException.class, () -> NumberUtils.toByteExact(null));
        assertThrows(NullPointerException.class, () -> NumberUtils.toShortExact(null));
        assertThrows(NullPointerException.class, () -> NumberUtils.toIntExact(null));
        assertThrows(NullPointerException.class, () -> NumberUtils.toLongExact(null));
        assertThrows(NullPointerException.class, () -> NumberUtils.toBigIntegerExact(null));
        assertThrows(NullPointerException.class, () -> NumberUtils.toBigDecimal((Number) null));
    }
}
