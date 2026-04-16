package io.github.connellite.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NumberUtilsTest {

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
}
