package io.github.connellite.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConvertToAsciiTest {

    @Test
    void native2asciiEscapesNonAscii() {
        assertEquals("123 test \\u0442\\u0435\\u0441\\u0442", ConvertToAscii.native2ascii("123 test тест", false));
    }

    @Test
    void ascii2nativeDecodesEscapes() {
        assertEquals("123 test тест", ConvertToAscii.ascii2native("123 test \\u0442\\u0435\\u0441\\u0442"));
    }

    @Test
    void native2asciiIgnoreLatinEscapesAll() {
        assertEquals("\\u0031\\u0032\\u0033", ConvertToAscii.native2ascii("123", true));
    }

    @Test
    void nullAndEmptyPassThrough() {
        assertNull(ConvertToAscii.native2ascii(null));
        assertNull(ConvertToAscii.ascii2native(null));
        assertEquals("", ConvertToAscii.native2ascii(""));
        assertEquals("", ConvertToAscii.ascii2native(""));
    }

    @Test
    void roundTrip() {
        String original = "café 测试";
        assertEquals(original, ConvertToAscii.ascii2native(ConvertToAscii.native2ascii(original, false)));
    }
}
