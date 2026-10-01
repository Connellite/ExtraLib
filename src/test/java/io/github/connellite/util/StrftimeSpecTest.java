package io.github.connellite.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DecimalStyle;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Conformance tests for the POSIX {@code strftime} engine, checked against the output a glibc
 * {@code date} produces for the same inputs.
 */
class StrftimeSpecTest {

    /** Saturday, day 167 of a leap year, so every week and day-of-year formula has work to do. */
    private static final ZonedDateTime SAMPLE =
            ZonedDateTime.of(2024, 6, 15, 14, 30, 45, 0, ZoneOffset.ofHours(3));

    private static final Locale JAPANESE_ERA = Locale.forLanguageTag("ja-JP-u-ca-japanese");
    private static final Locale ARABIC_DIGITS = Locale.forLanguageTag("ar-EG");
    private static final Locale RUSSIAN = Locale.forLanguageTag("ru-RU");

    private static String fmt(String pattern) {
        return DateTimeUtilFormat.strftime(Locale.US, SAMPLE, pattern);
    }

    private static String fmt(Locale locale, ZonedDateTime when, String pattern) {
        return DateTimeUtilFormat.strftime(locale, when, pattern);
    }

    private static ZonedDateTime atMidnight(int year, int month, int day) {
        return ZonedDateTime.of(year, month, day, 0, 0, 0, 0, ZoneOffset.UTC);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', value = {
            "%a|Sat",
            "%A|Saturday",
            "%b|Jun",
            "%h|Jun",
            "%B|June",
            "%c|Sat Jun 15 14:30:45 2024",
            "%C|20",
            "%d|15",
            "%D|06/15/24",
            "%e|15",
            "%F|2024-06-15",
            "%g|24",
            "%G|2024",
            "%H|14",
            "%I|02",
            "%j|167",
            "%k|14",
            "%l|' 2'",
            "%m|06",
            "%M|30",
            "%p|PM",
            "%P|pm",
            "%r|02:30:45 PM",
            "%R|14:30",
            "%S|45",
            "%T|14:30:45",
            "%u|6",
            "%U|23",
            "%V|24",
            "%w|6",
            "%W|24",
            "%x|6/15/24",
            "%X|2:30:45 PM",
            "%y|24",
            "%Y|2024",
            "%z|+0300",
            "%%|%",
    })
    void posixConversions(String pattern, String expected) {
        assertEquals(expected, fmt(pattern));
    }

    @Test
    void whitespaceConversions() {
        assertEquals("\n", fmt("%n"));
        assertEquals("\t", fmt("%t"));
    }

    @Test
    void epochSeconds() {
        assertEquals(Long.toString(SAMPLE.toEpochSecond()), fmt("%s"));
    }

    @Test
    void literalTextIsCopiedThrough() {
        assertEquals("on Sat, 15 Jun 2024", fmt("on %a, %d %b %Y"));
    }

    // --- flags and field width -------------------------------------------------------------

    @ParameterizedTest(name = "{0} -> ''{1}''")
    @CsvSource(delimiter = '|', value = {
            "%-d|15",
            "%-j|167",
            "%_m|' 6'",
            "%0e|15",
            "%_e|15",
            "%^a|SAT",
            "%^B|JUNE",
            "%#p|pm",
            "%#A|SATURDAY",
            "%10Y|0000002024",
            "%_10Y|'      2024'",
            "%-10Y|'      2024'",
            "%4m|0006",
            "%_4m|'   6'",
            "%1j|167",
    })
    void flagsAndWidth(String pattern, String expected) {
        assertEquals(expected, fmt(pattern));
    }

    @Test
    @DisplayName("a day below ten shows the difference between %d, %e and the padding flags")
    void singleDigitPadding() {
        ZonedDateTime fifth = ZonedDateTime.of(2024, 6, 5, 9, 0, 0, 0, ZoneOffset.UTC);
        assertEquals("05", fmt(Locale.US, fifth, "%d"));
        assertEquals(" 5", fmt(Locale.US, fifth, "%e"));
        assertEquals("5", fmt(Locale.US, fifth, "%-d"));
        assertEquals(" 5", fmt(Locale.US, fifth, "%_d"));
        assertEquals("05", fmt(Locale.US, fifth, "%0e"));
        assertEquals("5", fmt(Locale.US, fifth, "%-e"));
        assertEquals(" 9", fmt(Locale.US, fifth, "%l"));
        assertEquals(" 9", fmt(Locale.US, fifth, "%k"));
    }

    @Test
    @DisplayName("an absurd field width is capped instead of exhausting memory")
    void hugeFieldWidthIsClamped() {
        assertEquals(8192, fmt("%99999999999999d").length());
        assertEquals(8192, fmt("%9000d").length());
    }

    // --- glibc minimum widths --------------------------------------------------------------

    @Test
    @DisplayName("%Y and %G have a minimum width of one, as in glibc rather than POSIX")
    void yearIsNotPaddedToFourDigits() {
        assertEquals("5", fmt(Locale.US, atMidnight(5, 3, 1), "%Y"));
        assertEquals("05", fmt(Locale.US, atMidnight(5, 3, 1), "%y"));
        assertEquals("0", fmt(Locale.US, atMidnight(5, 3, 1), "%C"));
    }

    @Test
    void yearsAboveFourDigits() {
        ZonedDateTime far = atMidnight(12345, 6, 15);
        assertEquals("12345", fmt(Locale.US, far, "%Y"));
        assertEquals("123", fmt(Locale.US, far, "%C"));
        assertEquals("45", fmt(Locale.US, far, "%y"));
    }

    @Test
    @DisplayName("a negative year rounds its century away from zero, as glibc does")
    void negativeYears() {
        ZonedDateTime bc = atMidnight(-1, 6, 15);
        assertEquals("-1", fmt(Locale.US, bc, "%Y"));
        assertEquals("-1", fmt(Locale.US, bc, "%C"));
        assertEquals("99", fmt(Locale.US, bc, "%y"));
        assertEquals("-0001", fmt(Locale.US, bc, "%5Y"));
    }

    @Test
    @DisplayName("the POSIX + flag signs a year that outgrows its field, which glibc itself lacks")
    void plusFlag() {
        assertEquals("2024", fmt("%+Y"));
        assertEquals("+12345", fmt(Locale.US, atMidnight(12345, 6, 15), "%+Y"));
        assertEquals("+123", fmt(Locale.US, atMidnight(12345, 6, 15), "%+C"));
        assertEquals("0005", fmt(Locale.US, atMidnight(5, 3, 1), "%+Y"));
        assertEquals("+12345-06-15", fmt(Locale.US, atMidnight(12345, 6, 15), "%+F"));
    }

    // --- unknown and forbidden specifiers --------------------------------------------------

    @ParameterizedTest(name = "{0} is echoed back")
    @ValueSource(strings = {
            "%Q", "%", "%0", "%-", "%E", "%O",
            "%E%", "%O%", "%Ea", "%EA", "%ED", "%EF", "%Oa", "%OA", "%OD", "%OF", "%O%",
            "%Eb", "%Eh", "%EB", "%Ed", "%Ee", "%EH", "%EI", "%Ek", "%El", "%Ej", "%EM",
            "%Em", "%ES", "%EU", "%EV", "%Eg", "%EG", "%EW", "%Ew",
            "%Oc", "%Ox", "%OX", "%OY",
    })
    void unknownAndForbiddenAreEchoedLiterally(String pattern) {
        assertEquals(pattern, fmt(pattern));
    }

    @Test
    @DisplayName("the echo of a bad specifier still obeys the flags it carried")
    void echoObeysItsOwnFlags() {
        assertEquals("%^Q", fmt("%^q"));
        assertEquals("   %5", fmt("%5"));
    }

    @ParameterizedTest(name = "{0} is accepted")
    @ValueSource(strings = {"%Ou", "%Oz", "%Op", "%OP", "%Os", "%OC", "%Oy", "%OZ", "%Er", "%ER", "%ET"})
    void modifiersGlibcDoesNotCheckAreAccepted(String pattern) {
        assertNotEquals(pattern, fmt(pattern));
    }

    // --- week numbering --------------------------------------------------------------------

    @ParameterizedTest(name = "{0}-{1}-{2} %U={3} %W={4}")
    @CsvSource({
            "2024, 1,  3, 00, 01",
            "2024, 1,  7, 01, 01",
            "2024, 1,  8, 01, 02",
            "2023, 1,  1, 01, 00",
            "2023, 1,  2, 01, 01",
            "2021, 1,  1, 00, 00",
    })
    void sundayAndMondayWeekNumbers(int year, int month, int day, String sunday, String monday) {
        ZonedDateTime when = atMidnight(year, month, day);
        assertEquals(sunday, fmt(Locale.US, when, "%U"));
        assertEquals(monday, fmt(Locale.US, when, "%W"));
    }

    @ParameterizedTest(name = "{0}-{1}-{2} is ISO {4}-W{3}")
    @CsvSource({
            // the first days of 2021 belong to the last week of 2020
            "2021, 1,  1, 53, 2020, 20",
            "2021, 1,  3, 53, 2020, 20",
            "2021, 1,  4, 01, 2021, 21",
            // the last days of 2019 already belong to 2020
            "2019, 12, 30, 01, 2020, 20",
            "2019, 12, 29, 52, 2019, 19",
            // and the last days of 2024 to 2025
            "2024, 12, 30, 01, 2025, 25",
            "2024, 12, 29, 52, 2024, 24",
            "2024, 1,   1, 01, 2024, 24",
    })
    void isoWeekDate(int year, int month, int day, String week, String weekYear, String shortWeekYear) {
        ZonedDateTime when = atMidnight(year, month, day);
        assertEquals(week, fmt(Locale.US, when, "%V"));
        assertEquals(weekYear, fmt(Locale.US, when, "%G"));
        assertEquals(shortWeekYear, fmt(Locale.US, when, "%g"));
    }

    @Test
    void dayOfWeekNumbering() {
        assertEquals("7", fmt(Locale.US, atMidnight(2024, 6, 16), "%u"));
        assertEquals("0", fmt(Locale.US, atMidnight(2024, 6, 16), "%w"));
        assertEquals("1", fmt(Locale.US, atMidnight(2024, 6, 17), "%u"));
        assertEquals("1", fmt(Locale.US, atMidnight(2024, 6, 17), "%w"));
    }

    // --- offsets ----------------------------------------------------------------------------

    @ParameterizedTest(name = "offset {0}s -> {1}")
    @CsvSource({
            "0,      +0000",
            "10800,  +0300",
            "19800,  +0530",
            "20700,  +0545",
            "-12600, -0330",
            "50400,  +1400",
            "-39600, -1100",
    })
    void utcOffsets(int totalSeconds, String expected) {
        ZonedDateTime when = ZonedDateTime.of(2024, 6, 15, 12, 0, 0, 0, ZoneOffset.ofTotalSeconds(totalSeconds));
        assertEquals(expected, fmt(Locale.US, when, "%z"));
    }

    // --- E modifier ---------------------------------------------------------------------------

    @Test
    @DisplayName("E reaches the locale's own calendar where the JDK declares one")
    void eraConversionsInJapanese() {
        assertEquals("令和", fmt(JAPANESE_ERA, SAMPLE, "%EC"));
        assertEquals("令和6", fmt(JAPANESE_ERA, SAMPLE, "%EY"));
        assertEquals("06", fmt(JAPANESE_ERA, SAMPLE, "%Ey"));
        assertEquals("R6/6/15", fmt(JAPANESE_ERA, SAMPLE, "%Ex"));
        assertTrue(fmt(JAPANESE_ERA, SAMPLE, "%Ec").startsWith("令和6"));
        assertEquals(fmt(JAPANESE_ERA, SAMPLE, "%X"), fmt(JAPANESE_ERA, SAMPLE, "%EX"));
    }

    @Test
    @DisplayName("E falls back to the plain conversion where the locale has no era, as POSIX requires")
    void eraConversionsFallBackWithoutEras() {
        for (String conv : new String[]{"%EC", "%EY", "%Ey", "%Ex", "%Ec", "%EX"}) {
            assertEquals(fmt(conv.replace("E", "")), fmt(conv), conv);
        }
    }

    // --- O modifier ---------------------------------------------------------------------------

    @Test
    @DisplayName("O switches to the locale's own digits")
    void alternativeDigits() {
        char zero = DecimalStyle.of(ARABIC_DIGITS).getZeroDigit();
        assertNotEquals('0', zero, "the test locale is expected to carry its own digits");
        String one = String.valueOf((char) (zero + 1));
        String five = String.valueOf((char) (zero + 5));
        assertEquals(one + five, fmt(ARABIC_DIGITS, SAMPLE, "%Od"));
        assertEquals("15", fmt(ARABIC_DIGITS, SAMPLE, "%d"));
    }

    @Test
    @DisplayName("O falls back to plain digits where the locale has none of its own")
    void alternativeDigitsFallBack() {
        for (String conv : new String[]{"%Od", "%Oe", "%OH", "%OI", "%Om", "%OM", "%OS",
                "%Ou", "%OU", "%OV", "%Ow", "%OW", "%Oy"}) {
            assertEquals(fmt(conv.replace("O", "")), fmt(conv), conv);
        }
    }

    @Test
    @DisplayName("O on a month name selects the stand-alone form")
    void standaloneMonths() {
        assertEquals("июня", fmt(RUSSIAN, SAMPLE, "%B"));
        assertEquals("июнь", fmt(RUSSIAN, SAMPLE, "%OB"));
        assertEquals("июнь", fmt(RUSSIAN, SAMPLE, "%Ob"));
    }

    // --- facade -------------------------------------------------------------------------------

    @Test
    void nullValueRendersAsTheWordNull() {
        assertEquals("null", DateTimeUtilFormat.strftime(Locale.US, (ZonedDateTime) null, "%Y"));
        assertEquals("null", DateTimeUtilFormat.strftime(Locale.US, (Object) null, "%Y"));
    }

    @Test
    void localDateIsReadAtTheStartOfItsDay() {
        assertEquals("2024-06-15 00", DateTimeUtilFormat.strftime(
                Locale.US, LocalDate.of(2024, 6, 15), ZoneOffset.UTC, "%F %H"));
    }

    @Test
    void appendingStraightIntoABuffer() {
        StringBuilder sb = new StringBuilder("at ");
        DateTimeUtilFormat.strftimeTo(sb, Locale.US, SAMPLE, "%F");
        assertEquals("at 2024-06-15", sb.toString());
    }
}
