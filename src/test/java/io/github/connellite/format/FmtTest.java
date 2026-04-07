package io.github.connellite.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import io.github.connellite.exception.FormatException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class FmtTest {
    private final Locale previousDefaultLocale = Fmt.getDefaultLocale();

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

    @AfterEach
    void resetFmtLocale() {
        Fmt.setDefaultLocale(previousDefaultLocale);
    }

    @Test
    void basic() {
        assertEquals("hello 123\n", Fmt.format("hello {}\n", 123));
    }

    @Test
    void toStringUsesFmtPipeline() {
        assertEquals("42", Fmt.toString(42));
        assertEquals("null", Fmt.toString(null));
        assertEquals("[1, 2, 3]", Fmt.toString(new int[] {1, 2, 3}));
        assertEquals("[[1, 2], [3]]", Fmt.toString((Object) new int[][] {{1, 2}, {3}}));
        assertEquals("2024-06-15T14:30:45", Fmt.toString(LocalDateTime.of(2024, 6, 15, 14, 30, 45)));
        assertEquals("ab", Fmt.toString(new StringProxy("ab")));
    }

    @Test
    void basicConsumer() {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buf, true, StandardCharsets.UTF_8);
        Fmt.formatTo(out::println, "User {} logged in", "Alice");
        assertEquals("User Alice logged in" + System.lineSeparator(), buf.toString(StandardCharsets.UTF_8));
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
    void formatLocaleAwareNumberL() {
        assertEquals("1,000,000", Fmt.format(Locale.US, "{:L}", 1_000_000));
    }

    @Test
    void formatLocaleAwareNumberWithPrecisionSupportsBothOrders() {
        assertEquals("1,234.50", Fmt.format(Locale.US, "{:.2Lf}", 1234.5));
        assertEquals("1,234.50", Fmt.format(Locale.US, "{:L.2f}", 1234.5));
    }

    @Test
    void formatHexScientificFloatDouble() {
        assertEquals(String.format(Locale.US, "%a", 3.5), Fmt.format(Locale.US, "{:a}", 3.5));
        assertEquals(String.format(Locale.US, "%A", 3.5), Fmt.format(Locale.US, "{:A}", 3.5));
    }

    @Test
    void formatIeee754BitsFloatAndDouble() {
        float fv = 1.5f;
        double dv = 1.5d;
        String floatBits = String.format(
                Locale.ROOT,
                "%32s",
                Integer.toBinaryString(Float.floatToIntBits(fv))).replace(' ', '0');
        String doubleBits = String.format(
                Locale.ROOT,
                "%64s",
                Long.toBinaryString(Double.doubleToLongBits(dv))).replace(' ', '0');
        assertEquals(floatBits, Fmt.format("{:bits}", fv));
        assertEquals(doubleBits, Fmt.format("{:Bf}", dv));
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
    void formatDatePercentNamesAnd12Hour() {
        LocalDateTime t = LocalDateTime.of(2026, 4, 7, 14, 30, 45);
        assertEquals("Tuesday, April 07, 2026", Fmt.format(Locale.US, "{:%A, %B %d, %Y}", t));
        assertEquals("02:30:45 PM", Fmt.format(Locale.US, "{:%I:%M:%S %p}", t));
    }

    @Test
    void formatDatePercentIsoAndOffset() {
        ZonedDateTime now = ZonedDateTime.of(2026, 4, 7, 14, 30, 45, 0, ZonedDateTime.now().getZone());
        String expected = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssxx", Locale.US).format(now);
        assertEquals(expected, Fmt.format(Locale.US, "{:%F %T%z}", now));
    }

    @Test
    void formatDatePercentAliasesAndWeekSpecs() {
        LocalDateTime t = LocalDateTime.of(2026, 1, 1, 9, 8, 7);
        assertEquals("01/01/26", Fmt.format("{:%D}", t));
        assertEquals("Jan", Fmt.format(Locale.US, "{:%h}", t));
        assertEquals("2026-01-01 09:08:07", Fmt.format("{:%F %T}", t));

        int expectedU = expectedWeekNumberSundayFirst(t);
        int expectedW = expectedWeekNumberMondayFirst(t);
        assertEquals(String.format(Locale.ROOT, "%02d", expectedU), Fmt.format("{:%U}", t));
        assertEquals(String.format(Locale.ROOT, "%02d", expectedW), Fmt.format("{:%W}", t));
    }

    @Test
    void formatDatePercentIsoWeekYearVariants() {
        LocalDateTime t = LocalDateTime.of(2021, 1, 1, 10, 11, 12);
        int isoWeek = t.get(WeekFields.ISO.weekOfWeekBasedYear());
        int isoWeekYear = t.get(WeekFields.ISO.weekBasedYear());
        int isoWeekYear2 = Math.floorMod(isoWeekYear, 100);
        assertEquals(
                String.format(Locale.ROOT, "%02d/%04d/%02d", isoWeek, isoWeekYear, isoWeekYear2),
                Fmt.format("{:%V/%G/%g}", t));
    }

    @Test
    void formatDatePercentLocalizedDateTimeSpecs() {
        LocalDateTime t = LocalDateTime.of(2026, 4, 7, 14, 30, 45);
        String expectedDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(Locale.US).format(t);
        String expectedTime = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).withLocale(Locale.US).format(t);
        String expectedDateTime = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.US).format(t);
        assertEquals(expectedDate, Fmt.format(Locale.US, "{:%x}", t));
        assertEquals(expectedTime, Fmt.format(Locale.US, "{:%X}", t));
        assertEquals(expectedDateTime, Fmt.format(Locale.US, "{:%c}", t));
    }

    @Test
    void formatDatePercentWeekdayNumberSpecs() {
        LocalDateTime t = LocalDateTime.of(2026, 4, 5, 1, 2, 3);
        assertEquals("7/0", Fmt.format("{:%u/%w}", t));
    }

    @Test
    void formatDatePercentDayOfYearSpec() {
        LocalDateTime t = LocalDateTime.of(2024, 12, 31, 23, 59, 59);
        assertEquals("366", Fmt.format("{:%j}", t));
    }

    @Test
    void formatDatePercentZoneNameSpec() {
        ZonedDateTime t = ZonedDateTime.of(2026, 4, 7, 14, 30, 45, 0, ZonedDateTime.now().getZone());
        String expected = DateTimeFormatter.ofPattern("z", Locale.US).format(t);
        assertEquals(expected, Fmt.format(Locale.US, "{:%Z}", t));
    }

    @Test
    void defaultLocaleFromFmt() {
        Fmt.setDefaultLocale(Locale.US);
        assertEquals("1,000,000", Fmt.format("{:L}", 1_000_000));
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
        Fmt.formatTo(sb, "a{}b", 0);
        assertEquals(">>a0b", sb.toString());
    }

    @Test
    void formatToLocale() {
        StringBuilder sb = new StringBuilder();
        Fmt.formatTo(sb, Locale.US, "x={:.1f}", 2.25);
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
        Fmt.formatTo(buf, "a{}b", 0);
        assertEquals(">>a0b", buf.toString());
    }

    @Test
    void formatToConsumerMethodRefPrintln() {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buf, true, StandardCharsets.UTF_8);
        Fmt.formatTo(out::println, "a{}b", 0);
        String nl = System.lineSeparator();
        assertEquals("a0b" + nl, buf.toString(StandardCharsets.UTF_8));
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

    private static int expectedWeekNumberSundayFirst(LocalDateTime dt) {
        var date = dt.toLocalDate();
        var jan1 = date.withDayOfYear(1);
        var firstSunday = jan1.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        if (date.isBefore(firstSunday)) {
            return 0;
        }
        return (int) ((date.toEpochDay() - firstSunday.toEpochDay()) / 7) + 1;
    }

    private static int expectedWeekNumberMondayFirst(LocalDateTime dt) {
        var date = dt.toLocalDate();
        var jan1 = date.withDayOfYear(1);
        var firstMonday = jan1.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        if (date.isBefore(firstMonday)) {
            return 0;
        }
        return (int) ((date.toEpochDay() - firstMonday.toEpochDay()) / 7) + 1;
    }
}
