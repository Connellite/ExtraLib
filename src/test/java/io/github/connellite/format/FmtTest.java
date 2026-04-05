package io.github.connellite.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import io.github.connellite.exception.FormatException;
import org.junit.jupiter.api.Test;

class FmtTest {

    private record StringProxy(CharSequence data) implements FmtFormattable {

        @Override
        public void appendFormatted(Appendable out, Locale locale, String spec) throws IOException {
            String s = data.toString();
            if (spec == null || spec.isEmpty()) {
                out.append(s);
            } else {
                out.append(String.format(locale, "%" + spec, s));
            }
        }
    }

    @Test
    void basic() {
        assertEquals("hello 123\n", Fmt.format("hello {}\n", 123));
    }

    @Test
    void formatArrayDefaultAndNested() {
        assertEquals("[1, 2, 3]", Fmt.format("{}", (Object) new int[] {1, 2, 3}));
        // int[][] is an Object[] for varargs — cast so one argument is the matrix, not two rows.
        assertEquals("[[1, 2], [3]]", Fmt.format("{}", (Object) new int[][] {{1, 2}, {3}}));
        assertEquals(
                "[a, null, b]",
                Fmt.format("{}", (Object) new String[] {"a", null, "b"}));
        assertEquals("[1, 2, 3]", Fmt.format("{:s}", (Object) new int[] {1, 2, 3}));
    }

    @Test
    void boxTableDynamicWidth() {
        String expected =
                """
                        ┌────────────────────┐
                        │   Hello, world!    │
                        └────────────────────┘
                        """;
        assertEquals(
                expected,
                Fmt.format("┌{0:─^{2}}┐\n│{1: ^{2}}│\n└{0:─^{2}}┘\n", "", "Hello, world!", 20));
    }

    @Test
    void formatBinary() {
        assertEquals("101010", Fmt.format("{:b}", 42));
    }

    @Test
    void formatDynamicPrecision() {
        assertEquals("3.1", Fmt.format(Locale.US, "{:.{}f}", 3.1415, 1));
    }

    @Test
    void formatFloatSignFlags() {
        assertEquals(
                "+3.140000; -3.140000",
                Fmt.format(Locale.US, "{:+f}; {:+f}", 3.14, -3.14));
        assertEquals(
                " 3.140000; -3.140000",
                Fmt.format(Locale.US, "{: f}; {: f}", 3.14, -3.14));
        assertEquals(
                "3.140000; -3.140000",
                Fmt.format(Locale.US, "{:-f}; {:-f}", 3.14, -3.14));
    }

    @Test
    void formatRadixLine() {
        assertEquals(
                "int: 42;  hex: 2a;  oct: 52; bin: 101010",
                Fmt.format("int: {0:d};  hex: {0:x};  oct: {0:o}; bin: {0:b}", 42));
    }

    @Test
    void formatRadixAlternate() {
        assertEquals(
                "int: 42;  hex: 0x2a;  oct: 052;  bin: 0b101010",
                Fmt.format("int: {0:d};  hex: {0:#x};  oct: {0:#o};  bin: {0:#b}", 42));
    }

    @Test
    void formatDatePercent() {
        LocalDateTime t = LocalDateTime.of(2024, 6, 15, 14, 30, 45);
        assertEquals("2024-06-15 14:30:45", Fmt.format("{:%Y-%m-%d %H:%M:%S}", t));
    }

    @Test
    void formatDatePercentUtilDate() {
        @SuppressWarnings("deprecation")
        Date d = new Date(124, Calendar.JUNE, 15, 14, 30, 45);
        assertEquals("2024-06-15 14:30:45", Fmt.format("{:%Y-%m-%d %H:%M:%S}", d));
    }

    @Test
    void named() {
        assertEquals("a=1 b=2", Fmt.format("a={a} b={b}", Fmt.arg("a", 1), Fmt.arg("b", 2)));
    }

    @Test
    void namedFromMap() {
        assertEquals("1 + 2", Fmt.format("{a} + {b}", Map.of("a", 1, "b", 2)));
    }

    @Test
    void compileReuse() {
        CompiledFormat c = Fmt.compile("{} = {:d}");
        assertEquals("x = 1", Fmt.format(c, "x", 1));
        assertEquals("y = 2", Fmt.format(c, "y", 2));
    }

    @Test
    void mixed() {
        assertEquals("1 then 2", Fmt.format("{} then {x}", 1, Fmt.arg("x", 2)));
    }

    @Test
    void index() {
        assertEquals("z y", Fmt.format("{1} {0}", "y", "z"));
    }

    @Test
    void spec() {
        assertEquals("3.14", Fmt.format(Locale.US, "{:.2f}", 3.14159));
    }

    @Test
    void formatTo() {
        StringBuilder sb = new StringBuilder(">>");
        Fmt.format_to(sb, "a{}b", 0);
        assertEquals(">>a0b", sb.toString());
    }

    @Test
    void formatToLocale() {
        StringBuilder sb = new StringBuilder();
        Fmt.format_to(sb, Locale.US, "x={:.1f}", 2.25);
        assertEquals("x=2.3", sb.toString());
    }

    @Test
    void brace() {
        assertEquals("{a}", Fmt.format("{{a}}"));
    }

    @Test
    void closeBrace() {
        assertEquals("}", Fmt.format("}}"));
    }

    @Test
    void printPrintStream() {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        Fmt.print(new PrintStream(buf, true, StandardCharsets.UTF_8), "v{}", 9);
        assertEquals("v9", buf.toString(StandardCharsets.UTF_8));
    }

    @Test
    void formatToStringBuffer() {
        StringBuffer buf = new StringBuffer(">>");
        Fmt.format_to(buf, "a{}b", 0);
        assertEquals(">>a0b", buf.toString());
    }

    @Test
    void formatToConsumerMethodRefPrintln() {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buf, true, StandardCharsets.UTF_8);
        Fmt.format_to(out::println, "a{}b", 0);
        String nl = System.lineSeparator();
        assertEquals("a" + nl + "0" + nl + "b" + nl, buf.toString(StandardCharsets.UTF_8));
    }

    @Test
    void printlnWriter() {
        StringWriter sw = new StringWriter();
        Fmt.println(new PrintWriter(sw), "w{}", 1);
        assertEquals("w1" + System.lineSeparator(), sw.toString());
    }

    @Test
    void missingNamedThrows() {
        assertThrows(FormatException.class, () -> Fmt.format("{nope}", Fmt.arg("other", 1)));
    }

    @Test
    void unclosedThrows() {
        assertThrows(FormatException.class, () -> Fmt.format("{0", 1));
    }

    @Test
    void unmatchedBraceThrows() {
        assertThrows(FormatException.class, () -> Fmt.format("}", 1));
    }

    @Test
    void nullPatternThrows() {
        assertThrows(FormatException.class, () -> Fmt.format((CharSequence) null, 1));
    }

    @Test
    void nullSole() {
        assertEquals("null", Fmt.format("{}", (Object) null));
    }

    @Test
    void nullMixed() {
        assertEquals("a|null|c", Fmt.format("{}|{}|{}", "a", null, "c"));
    }

    @Test
    void nullNamed() {
        assertEquals("v=null", Fmt.format("v={v}", Fmt.arg("v", null)));
    }

    @Test
    void nullIndex() {
        assertEquals("x=null", Fmt.format("x={0}", (Object) null));
    }

    @Test
    void objectDefault() {
        Object plain = new Object();
        assertEquals(String.valueOf(plain), Fmt.format("{}", plain));
    }

    @Test
    void nullWithColonS() {
        assertEquals("null", Fmt.format(Locale.ROOT, "{:s}", (Object) null));
    }

    @Test
    void nullWithDspec() {
        assertEquals("null", Fmt.format("{:d}", (Object) null));
    }

    @Test
    void stringAsIntSpecThrows() {
        assertThrows(FormatException.class, () -> Fmt.format("{:d}", "x"));
    }

    @Test
    void fmtFormattableDefault() {
        assertEquals("ab", Fmt.format("{}", new StringProxy("ab")));
    }

    @Test
    void fmtFormattableWithSpec() {
        assertEquals("AB", Fmt.format(Locale.ROOT, "{:S}", new StringProxy("ab")));
    }

    @Test
    void fmtStyleAlignRightAndLeft() {
        assertEquals("         a b         ", Fmt.format("{0:>10} {1:<10}", "a", "b"));
    }

    @Test
    void fmtStyleAlternateHex() {
        assertEquals("0x2a", Fmt.format("{:#x}", 42));
    }

    @Test
    void fmtStyleCenter() {
        assertEquals("  hi   ", Fmt.format("{:^7}", "hi"));
    }

    @Test
    void fmtStyleFillAlign() {
        assertEquals("*****hi", Fmt.format("{:*>7}", "hi"));
    }
}
