package io.github.connellite.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import io.github.connellite.exception.FormatException;
import io.github.connellite.util.DateTimeUtilFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

    @ParameterizedTest
    @MethodSource("simpleSpecCases")
    void simpleSpecCases(String expected, String pattern, Object arg) {
        assertEquals(expected, Fmt.format(pattern, arg));
    }

    @Test
    void negativeWidthThrows() {
        assertThrows(FormatException.class, () -> Fmt.format("{:{}}", "hi", -5));
    }

    @Test
    void zeroWidth() {
        assertEquals("hi", Fmt.format("{:0}", "hi"));
    }

    @Test
    void localeAwareOnCharFormat() {
        assertEquals("A", Fmt.format(Locale.US, "{:Lc}", 65));
    }

    @Test
    void unclosedBraceInSpecThrows() {
        assertThrows(FormatException.class, () -> Fmt.format("{0:{1}", 42, 5));
    }

    @ParameterizedTest
    @MethodSource("invalidFormatCases")
    void invalidFormatCases(String pattern, Object arg) {
        assertThrows(FormatException.class, () -> Fmt.format(pattern, arg));
    }

    @Test
    void fullSpecComboFillAlignSignWidthPrecisionType() {
        assertEquals("+00003.142", Fmt.format(Locale.US, "{:0>+#10.3f}", 3.14159));
    }

    @Test
    void fullSpecComboCenterWithFill() {
        assertEquals("***3.14***", Fmt.format(Locale.US, "{:*^10.2f}", 3.14159));
    }

    @Test
    void fullSpecComboLeftAlignFillSign() {
        assertEquals("+3.14*****", Fmt.format(Locale.US, "{:*<+10.2f}", 3.14159));
    }

    @Test
    void namedArgWithNamedWidth() {
        assertEquals("   hi", Fmt.format("{msg:{w}s}",
                Fmt.arg("msg", "hi"), Fmt.arg("w", 5)));
    }

    @Test
    void hexFloatForInteger() {
        assertThrows(FormatException.class, () -> Fmt.format("{:a}", 42));
    }

    @Test
    void hexOctalNegative() {
        assertEquals("ffffffd6", Fmt.format("{:x}", -42));
        assertEquals("FFFFFFD6", Fmt.format("{:X}", -42));
        assertEquals("37777777726", Fmt.format("{:o}", -42));
    }

    @ParameterizedTest
    @MethodSource("zeroFlagCases")
    void zeroFlagCases(String expected, String pattern, int value) {
        assertEquals(expected, Fmt.format(pattern, value));
    }

    @Test
    void dynamicWidthNamed() {
        assertEquals("  42", Fmt.format("{0:{width}}", 42, Fmt.arg("width", 4)));
    }

    @Test
    void dynamicWidthNamedWithSpecOnValue() {
        assertEquals("  42", Fmt.format("{0:{width}d}", 42, Fmt.arg("width", 4)));
    }

    @Test
    void dynamicPrecisionNamed() {
        assertEquals("3.14", Fmt.format(Locale.US, "{0:.{prec}f}", 3.14159, Fmt.arg("prec", 2)));
    }

    @Test
    void dynamicWidthAndPrecision() {
        assertEquals("   3.14", Fmt.format(Locale.US, "{0:{w}.{p}f}", 3.14159, Fmt.arg("w", 7), Fmt.arg("p", 2)));
    }

    @Test
    void dashFlagThrowsOrConvertsToLeftAlign() {
        FormatException ex = assertThrows(FormatException.class,
                () -> Fmt.format("{:-10}", "hi"));
        assertTrue(ex.getMessage().contains("'-'") || ex.getMessage().contains("align"), "Error must mention '-' flag, not silently ignored");
    }

    @Test
    void alternateWithZeroPrecisionFloat() {
        assertEquals("42.", Fmt.format(Locale.US, "{:#.0f}", 42.0));
    }

    @Test
    void percentSpecWithNonDateThrows() {
        assertThrows(FormatException.class, () -> Fmt.format("{:%Y}", 42));
        assertThrows(FormatException.class, () -> Fmt.format("{:%Y}", "2024"));
    }

    @Test
    void localeAwareOnString() {
        assertEquals("hello", Fmt.format(Locale.US, "{:L}", "hello"));
    }

    @Test
    void localeAwareOnChar() {
        assertEquals("A", Fmt.format(Locale.US, "{:L}", 'A'));
    }

    @Test
    void largeWidth() {
        String s = Fmt.format("{:10000}", 7);
        assertEquals(10000, s.length());
    }

    @Test
    void strftimeUnknownConversionThrows() {
        assertThrows(FormatException.class, () -> Fmt.format("{:%Q}", LocalDate.now()));
    }

    @Test
    void nestedFormatCalls() {
        String inner = Fmt.format("[{}]", "x");
        assertEquals("outer: [x]", Fmt.format("outer: {}", inner));
    }

    @Test
    void strftimeForAllDateTypes() {
        String expected = "2024-06-15";
        assertEquals(expected, Fmt.format("{:%Y-%m-%d}", LocalDate.of(2024, 6, 15)));
        assertEquals(expected, Fmt.format("{:%Y-%m-%d}", LocalDateTime.of(2024, 6, 15, 0, 0)));
        assertEquals(expected, Fmt.format("{:%Y-%m-%d}", ZonedDateTime.of(2024, 6, 15, 0, 0, 0, 0, ZoneOffset.UTC)));
        assertEquals(expected, Fmt.format("{:%Y-%m-%d}", OffsetDateTime.of(2024, 6, 15, 0, 0, 0, 0, ZoneOffset.UTC)));
        assertEquals(expected, Fmt.format("{:%Y-%m-%d}", Instant.parse("2024-06-15T00:00:00Z")));
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
    void formatCharacterFromCode() {
        assertEquals("A", Fmt.format("{:c}", 65));
        assertEquals("A", Fmt.format("{:c}", (byte) 65));
        assertEquals("A", Fmt.format("{:c}", 'A'));
        assertEquals("\uD83D\uDE00", Fmt.format("{:c}", 0x1F600));
        assertEquals("\uD83D\uDE00", Fmt.format("{:c}", BigInteger.valueOf(0x1F600)));
    }

    @Test
    void formatCharacterFromCodeInvalidThrows() {
        assertThrows(FormatException.class, () -> Fmt.format("{:c}", 0x110000));
        assertThrows(FormatException.class, () -> Fmt.format("{:c}", "x"));
        assertThrows(FormatException.class, () -> Fmt.format("{:c}", 3.14));
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
        // %c follows C-style "%a %b %e %H:%M:%S %Y", not Java FormatStyle.MEDIUM
        String expectedDateTime = DateTimeUtilFormat.strftime(Locale.US, t, "%c");
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
        assertEquals("   +3.14", Fmt.format(Locale.US, "{:+8.2f}", 3.14));
        assertEquals("    42", Fmt.format("{:{}}", 42, 6));
        assertEquals("+42", Fmt.format("{:+}", 42));
        assertEquals("+42", Fmt.format("{:+d}", 42));
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

    /** {@code {}} → {@link AutoArgId}; consumer path must match {@link Fmt#format} for positional args. */
    @Test
    void formatToConsumer_autoArgIds_zeroThroughFive() {
        List<String> sink = new ArrayList<>();

        Fmt.formatTo(sink::add, "no placeholders");
        assertEquals(List.of("no placeholders"), sink);
        sink.clear();

        Fmt.formatTo(sink::add, "{}", "only");
        assertEquals(List.of("only"), sink);
        sink.clear();

        Fmt.formatTo(sink::add, "{} {}", 1, 2);
        assertEquals(List.of("1 2"), sink);
        sink.clear();

        Fmt.formatTo(sink::add, "{}|{}|{}", "a", "b", "c");
        assertEquals(List.of("a|b|c"), sink);
        sink.clear();

        Fmt.formatTo(sink::add, "{} {} {} {}", 10, 20, 30, 40);
        assertEquals(List.of("10 20 30 40"), sink);
        sink.clear();

        Fmt.formatTo(sink::add, "{} {} {} {} {}", 'p', 'q', 'r', 's', 't');
        assertEquals(List.of("p q r s t"), sink);
        sink.clear();
    }

    @Test
    void formatToConsumer_autoArgIds_emptyPlaceholderWithoutArgsThrows() {
        List<String> sink = new ArrayList<>();
        FormatException ex = assertThrows(FormatException.class, () -> Fmt.formatTo(sink::add, "{}"));
        assertEquals("argument not found: 0", ex.getMessage());
        assertEquals(List.of(), sink);
    }

    @Test
    void formatToConsumer_autoArgIds_notEnoughPositionalArgsThrows() {
        List<String> sink = new ArrayList<>();
        FormatException ex = assertThrows(FormatException.class, () -> Fmt.formatTo(sink::add, "{} {}", 1));
        assertEquals("argument not found: 1", ex.getMessage());
    }

    @Test
    void formatToConsumer_autoArgIds_skipsNamedInVarargs() {
        List<String> sink = new ArrayList<>();
        Fmt.formatTo(sink::add, "{}{x}{}", 1, Fmt.arg("x", "mid"), 2);
        assertEquals(List.of("1mid2"), sink);
    }

    @Test
    void formatToConsumer_mapAsPositionalArg() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("k-a", List.of("u1", "u2"));
        map.put("k-b", List.of());

        List<String> sink = new ArrayList<>();
        Fmt.formatTo(sink::add, "map: {}", map);

        assertEquals(List.of("map: " + map), sink);
    }

    @Test
    void formatToConsumer_instantAsPositionalArg() {
        Instant t = Instant.parse("2024-06-15T10:30:00Z");
        List<String> sink = new ArrayList<>();
        Fmt.formatTo(sink::add, "at: {}", t);
        assertEquals(List.of("at: " + t), sink);
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

    private static Stream<Arguments> simpleSpecCases() {
        return Stream.of(
                Arguments.of("42", "{:}", 42),
                Arguments.of("hi", "{:}", "hi"),
                Arguments.of("hi", "{:0}", "hi")
        );
    }

    private static Stream<Arguments> invalidFormatCases() {
        return Stream.of(
                Arguments.of("{:+s}", "hello"),
                Arguments.of("{: s}", "hello"),
                Arguments.of("{:#s}", "hello"),
                Arguments.of("{:b}", true),
                Arguments.of("{:b}", 3.14),
                Arguments.of("{:b}", BigDecimal.ONE)
        );
    }

    private static Stream<Arguments> zeroFlagCases() {
        return Stream.of(
                Arguments.of("00042", "{:05d}", 42),
                Arguments.of("42   ", "{:<05d}", 42),
                Arguments.of(" 42  ", "{:^05d}", 42),
                Arguments.of("+0042", "{:+05d}", 42),
                Arguments.of("-0042", "{:-05d}", -42)
        );
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
