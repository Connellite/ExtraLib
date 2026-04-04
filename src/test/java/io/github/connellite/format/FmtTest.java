package io.github.connellite.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

import io.github.connellite.exception.FormatException;
import org.junit.jupiter.api.Test;

class FmtTest {

    private static final class StringProxy implements FmtFormattable {
        private final CharSequence data;

        StringProxy(CharSequence data) {
            this.data = data;
        }

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
    void named() {
        assertEquals("a=1 b=2", Fmt.format("a={a} b={b}", Fmt.arg("a", 1), Fmt.arg("b", 2)));
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
    void printStream() {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        Fmt.print(new PrintStream(buf, true), "v{}", 9);
        assertEquals("v9", buf.toString());
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
        assertThrows(FormatException.class, () -> Fmt.format(null, 1));
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
}
