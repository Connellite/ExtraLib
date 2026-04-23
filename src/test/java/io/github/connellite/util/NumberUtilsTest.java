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
        assertEquals(Boolean.FALSE, NumberUtils.toBoolean("false"));
        assertEquals(Boolean.FALSE, NumberUtils.toBoolean("0"));
    }

    @Test
    void toBoolean_fromString_nullBlankInvalid() {
        assertNull(NumberUtils.toBoolean(null));
        assertNull(NumberUtils.toBoolean(" "));
        assertNull(NumberUtils.toBoolean("maybe"));
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
}
