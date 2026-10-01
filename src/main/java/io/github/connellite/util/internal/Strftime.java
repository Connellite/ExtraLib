package io.github.connellite.util.internal;

import java.time.chrono.ChronoLocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Locale;

/**
 * A port of the POSIX {@code strftime} engine from glibc, writing straight into a
 * {@link StringBuilder}.
 *
 * <p>The structure follows
 * <a href="https://sourceware.org/git/?p=glibc.git;a=blob;f=time/strftime_l.c;hb=HEAD#l548">__strftime_internal</a>
 * closely enough to be read side by side with it: one pass over the pattern, per-specifier state
 * for the flags, field width and {@code E}/{@code O} modifier, and two shared number emitters that
 * the numeric conversions jump to the way the C code jumps to {@code do_number} and
 * {@code do_number_spacepad}. Unknown conversions are echoed back literally, as glibc's
 * {@code bad_format} does, rather than reported as errors.
 *
 * <p>Besides the conversions POSIX requires, the GNU flags {@code - _ 0 ^ #}, the field width, and
 * the extensions {@code %P %k %l %s} are supported. The POSIX {@code +} flag is supported as well,
 * which glibc itself does not parse.
 *
 * <p>Three places adapt glibc's locale database to what the JDK exposes, and so are ports in spirit
 * rather than in letter:
 * <ul>
 *   <li>glibc's {@code nl_get_alt_digit} returns a whole alternative numeral per value; the JDK only
 *       offers a zero digit through {@link java.time.format.DecimalStyle}, so {@code O} shifts each
 *       rendered digit instead, and keeps the padding glibc's alternative path skips.</li>
 *   <li>glibc's {@code era_format} is a locale-supplied sub-pattern; the JDK has no equivalent
 *       field, so {@code %EY} formats through a CLDR era-and-year pattern.</li>
 *   <li>{@code %c} uses glibc's C-locale fallback {@code "%a %b %e %H:%M:%S %Y"} rather than a
 *       localized date-time pattern, which is the layout this library has always produced, while
 *       {@code %x} and {@code %X} are localized.</li>
 * </ul>
 *
 * <p>An instance holds the scratch buffer and the per-specifier state, so it is not thread-safe and
 * is meant to be created, used once and discarded.
 */
public final class Strftime {

    /** Monday, glibc's {@code ISO_WEEK_START_WDAY}. */
    private static final int ISO_WEEK_START_WDAY = 1;

    /** Thursday, glibc's {@code ISO_WEEK1_WDAY}. */
    private static final int ISO_WEEK1_WDAY = 4;

    /** Large enough that the modulo below never sees a negative operand, glibc's name for it. */
    private static final int BIG_ENOUGH_MULTIPLE_OF_7 = (366 / 7 + 2) * 7;

    /**
     * Largest field width honoured. glibc is told how much room it has and gives up when a width
     * would overrun it; writing into a {@link StringBuilder} there is no such bound, so a pattern
     * like {@code %99999999999999d} would otherwise try to allocate its way out of memory.
     */
    private static final int MAX_WIDTH = 8192;

    /** Room for the longest {@code long} plus a sign. */
    private final char[] buf = new char[24];

    private final BrokenDownTime time = new BrokenDownTime();

    private StringBuilder out;
    private CalendarText text;

    /** Padding selected by a flag: {@code '-'}, {@code '_'}, {@code '0'}, or zero for none. */
    private char pad;

    /** Minimum field width, or {@code -1} when the specifier gave none. */
    private int width;

    /** {@code 'E'}, {@code 'O'}, or zero. */
    private char modifier;

    private boolean plus;
    private boolean toUpper;
    private boolean toLower;
    private boolean changeCase;

    public Strftime() {
    }

    /**
     * @return the fields to format, to be filled by the caller before {@link #format}
     */
    public BrokenDownTime time() {
        return time;
    }

    /**
     * Formats {@link #time()} into {@code out} according to {@code pattern}.
     */
    public void format(StringBuilder out, CharSequence pattern, Locale locale) {
        this.out = out;
        this.text = CalendarText.of(locale);
        run(pattern);
    }

    private void run(CharSequence pattern) {
        int n = pattern.length();
        int i = 0;
        while (i < n) {
            char c = pattern.charAt(i);
            if (c != '%') {
                out.append(c);
                i++;
                continue;
            }

            pad = 0;
            width = -1;
            modifier = 0;
            plus = false;
            toUpper = false;
            toLower = false;
            changeCase = false;

            int j = scanSpec(pattern, i + 1);
            if (j >= n) {
                // a specifier that runs off the end is echoed whole, as glibc's "% at end of format"
                copySequence(pattern, i, n);
                return;
            }
            convert(pattern, i, j);
            i = j + 1;
        }
    }

    /**
     * Consumes the flags, the field width and the modifier that follow a {@code '%'}.
     *
     * @param from index just past the {@code '%'}
     * @return index of the conversion character, which may be past the end of {@code pattern}
     */
    private int scanSpec(CharSequence pattern, int from) {
        int n = pattern.length();
        int i = from;
        while (i < n) {
            char c = pattern.charAt(i);
            if (c == '_' || c == '-' || c == '0') {
                pad = c;
            } else if (c == '+') {
                // POSIX spells this flag out; glibc's flag loop does not accept it
                pad = '0';
                plus = true;
            } else if (c == '^') {
                toUpper = true;
            } else if (c == '#') {
                changeCase = true;
            } else {
                break;
            }
            i++;
        }

        if (i < n && isDigit(pattern.charAt(i))) {
            width = 0;
            do {
                width = Math.min(width * 10 + (pattern.charAt(i) - '0'), MAX_WIDTH);
                i++;
            } while (i < n && isDigit(pattern.charAt(i)));
        }

        if (i < n) {
            char c = pattern.charAt(i);
            if (c == 'E' || c == 'O') {
                modifier = c;
                i++;
            }
        }
        return i;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private void convert(CharSequence pattern, int specStart, int convPos) {
        char conv = pattern.charAt(convPos);
        switch (conv) {
            case '%' -> {
                if (modifier != 0) {
                    badFormat(pattern, specStart, convPos);
                    return;
                }
                addChar('%');
            }
            case 'a' -> {
                if (modifier != 0) {
                    badFormat(pattern, specStart, convPos);
                    return;
                }
                upperOnChangeCase();
                copyString(text.shortWeekday(time.dayOfWeek()));
            }
            case 'A' -> {
                if (modifier != 0) {
                    badFormat(pattern, specStart, convPos);
                    return;
                }
                upperOnChangeCase();
                copyString(text.fullWeekday(time.dayOfWeek()));
            }
            case 'b', 'h' -> {
                upperOnChangeCase();
                if (modifier == 'E') {
                    badFormat(pattern, specStart, convPos);
                    return;
                }
                copyString(text.shortMonth(time.month, modifier == 'O'));
            }
            case 'B' -> {
                if (modifier == 'E') {
                    badFormat(pattern, specStart, convPos);
                    return;
                }
                upperOnChangeCase();
                copyString(text.fullMonth(time.month, modifier == 'O'));
            }
            case 'c' -> {
                if (modifier == 'O') {
                    badFormat(pattern, specStart, convPos);
                    return;
                }
                if (modifier == 'E' && text.hasEras()) {
                    copyFormatted(text.eraDateTimeFormat(), time.temporal());
                    return;
                }
                subformat("%a %b %e %H:%M:%S %Y");
            }
            case 'C' -> {
                if (modifier == 'E' && text.hasEras()) {
                    copyString(eraDate().getEra().getDisplayName(TextStyle.FULL, text.locale()));
                    return;
                }
                // a negative year belongs to the century below the truncated quotient
                doNumber(1, time.year / 100 - (time.year % 100 < 0 ? 1 : 0), 2);
            }
            case 'x' -> {
                if (modifier == 'O') {
                    badFormat(pattern, specStart, convPos);
                    return;
                }
                copyFormatted(
                        modifier == 'E' && text.hasEras() ? text.eraDateFormat() : text.dateFormat(),
                        time.temporal());
            }
            case 'D' -> {
                if (modifier != 0) {
                    badFormat(pattern, specStart, convPos);
                    return;
                }
                subformat("%m/%d/%y");
            }
            case 'd' -> {
                if (modifier == 'E') {
                    badFormat(pattern, specStart, convPos);
                    return;
                }
                doNumber(2, time.day, 0);
            }
            case 'e' -> {
                if (modifier == 'E') {
                    badFormat(pattern, specStart, convPos);
                    return;
                }
                doNumberSpacePad(2, time.day);
            }
            case 'F' -> {
                if (modifier != 0) {
                    badFormat(pattern, specStart, convPos);
                    return;
                }
                subformat(plus ? "%+Y-%m-%d" : "%Y-%m-%d");
            }
            case 'H' -> {
                if (modifier == 'E') {
                    badFormat(pattern, specStart, convPos);
                    return;
                }
                doNumber(2, time.hour, 0);
            }
            case 'I' -> {
                if (modifier == 'E') {
                    badFormat(pattern, specStart, convPos);
                    return;
                }
                doNumber(2, time.hour12(), 0);
            }
            case 'k' -> {
                if (modifier == 'E') {
                    badFormat(pattern, specStart, convPos);
                    return;
                }
                doNumberSpacePad(2, time.hour);
            }
            case 'l' -> {
                if (modifier == 'E') {
                    badFormat(pattern, specStart, convPos);
                    return;
                }
                doNumberSpacePad(2, time.hour12());
            }
            case 'j' -> {
                if (modifier == 'E') {
                    badFormat(pattern, specStart, convPos);
                    return;
                }
                doNumber(3, 1 + time.dayOfYear(), 0);
            }
            case 'M' -> {
                if (modifier == 'E') {
                    badFormat(pattern, specStart, convPos);
                    return;
                }
                doNumber(2, time.minute, 0);
            }
            case 'm' -> {
                if (modifier == 'E') {
                    badFormat(pattern, specStart, convPos);
                    return;
                }
                doNumber(2, time.month, 0);
            }
            case 'n' -> addChar('\n');
            case 'P', 'p' -> {
                if (conv == 'P' || changeCase) {
                    toUpper = false;
                    toLower = true;
                }
                copyString(text.amPm(time.hour));
            }
            case 'R' -> subformat("%H:%M");
            case 'r' -> subformat("%I:%M:%S %p");
            case 'S' -> {
                if (modifier == 'E') {
                    badFormat(pattern, specStart, convPos);
                    return;
                }
                doNumber(2, time.second, 0);
            }
            case 's' -> doNumber(1, time.epochSecond(), 0);
            case 'X' -> {
                if (modifier == 'O') {
                    badFormat(pattern, specStart, convPos);
                    return;
                }
                copyFormatted(
                        modifier == 'E' && text.hasEras() ? text.eraTimeFormat() : text.timeFormat(),
                        time.temporal());
            }
            case 'T' -> subformat("%H:%M:%S");
            case 't' -> addChar('\t');
            case 'u' -> doNumber(1, (time.dayOfWeek() - 1 + 7) % 7 + 1, 0);
            case 'U' -> {
                if (modifier == 'E') {
                    badFormat(pattern, specStart, convPos);
                    return;
                }
                doNumber(2, (time.dayOfYear() - time.dayOfWeek() + 7) / 7, 0);
            }
            case 'V', 'g', 'G' -> {
                if (modifier == 'E') {
                    badFormat(pattern, specStart, convPos);
                    return;
                }
                weekBasedYear(conv);
            }
            case 'W' -> {
                if (modifier == 'E') {
                    badFormat(pattern, specStart, convPos);
                    return;
                }
                doNumber(2, (time.dayOfYear() - (time.dayOfWeek() - 1 + 7) % 7 + 7) / 7, 0);
            }
            case 'w' -> {
                if (modifier == 'E') {
                    badFormat(pattern, specStart, convPos);
                    return;
                }
                doNumber(1, time.dayOfWeek(), 0);
            }
            case 'Y' -> {
                if (modifier == 'E' && text.hasEras()) {
                    copyFormatted(text.eraYearFormat(), time.temporal());
                    return;
                }
                if (modifier == 'O') {
                    badFormat(pattern, specStart, convPos);
                    return;
                }
                doNumber(1, time.year, 4);
            }
            case 'y' -> {
                if (modifier == 'E' && text.hasEras()) {
                    doNumber(2, eraDate().get(ChronoField.YEAR_OF_ERA), 0);
                    return;
                }
                doNumber(2, (time.year % 100 + 100) % 100, 0);
            }
            case 'Z' -> {
                if (changeCase) {
                    toUpper = false;
                    toLower = true;
                }
                copyFormatted(text.zoneFormat(), time.temporal());
            }
            case 'z' -> {
                int diff = time.offsetSeconds;
                if (diff < 0) {
                    addChar('-');
                    diff = -diff;
                } else {
                    addChar('+');
                }
                diff /= 60;
                doNumber(4, (diff / 60) * 100 + diff % 60, 0);
            }
            default -> badFormat(pattern, specStart, convPos);
        }
    }

    /**
     * The shared body of {@code %V}, {@code %g} and {@code %G}: find the year the ISO week belongs
     * to, which may be the one before or after the calendar year.
     */
    private void weekBasedYear(char conv) {
        int year = time.year;
        int days = isoWeekDays(time.dayOfYear(), time.dayOfWeek());

        if (days < 0) {
            year--;
            days = isoWeekDays(time.dayOfYear() + yearLength(year), time.dayOfWeek());
        } else {
            int d = isoWeekDays(time.dayOfYear() - yearLength(year), time.dayOfWeek());
            if (d >= 0) {
                year++;
                days = d;
            }
        }

        switch (conv) {
            case 'g' -> doNumber(2, (year % 100 + 100) % 100, 0);
            case 'G' -> doNumber(1, year, 4);
            default -> doNumber(2, days / 7 + 1, 0);
        }
    }

    /**
     * Days since the start of the ISO week-based year containing {@code yday}, which is negative
     * when the day belongs to the previous year's last week.
     *
     * @see <a href="https://sourceware.org/git/?p=glibc.git;a=blob;f=time/strftime_l.c;hb=HEAD#l466">iso_week_days</a>
     */
    private static int isoWeekDays(int yday, int wday) {
        return yday
                - (yday - wday + ISO_WEEK1_WDAY + BIG_ENOUGH_MULTIPLE_OF_7) % 7
                + ISO_WEEK1_WDAY - ISO_WEEK_START_WDAY;
    }

    private static int yearLength(int year) {
        return BrokenDownTime.isLeapYear(year) ? 366 : 365;
    }

    private ChronoLocalDate eraDate() {
        return text.chronology().date(time.toLocalDate());
    }

    private void upperOnChangeCase() {
        if (changeCase) {
            toUpper = true;
            toLower = false;
        }
    }

    /**
     * Runs a nested pattern, as glibc's {@code subformat} label does, then applies this specifier's
     * width and case to the whole of its output.
     */
    private void subformat(String subfmt) {
        int savedWidth = width;
        char savedPad = pad;
        boolean savedUpper = toUpper;
        boolean savedLower = toLower;

        int mark = out.length();
        run(subfmt);

        width = savedWidth;
        pad = savedPad;
        toUpper = savedUpper;
        toLower = savedLower;
        padBefore(mark);
        applyCase(mark);
    }

    /**
     * Forces space padding unless a flag already asked for zeros or for none, as glibc's
     * {@code do_number_spacepad} label does.
     */
    private void doNumberSpacePad(int d, long value) {
        if (pad != '0' && pad != '-') {
            pad = '_';
        }
        doNumber(d, value, 0);
    }

    /**
     * Renders {@code value} right-aligned in at least {@code d} digits, or in the field width when
     * that is larger.
     *
     * @param plusMinimum the width above which the POSIX {@code +} flag emits a leading sign, or
     *                    zero for the conversions that flag does not apply to
     */
    private void doNumber(int d, long value, int plusMinimum) {
        boolean signed = plus && plusMinimum > 0;
        if (signed && d < plusMinimum) {
            d = plusMinimum;
        }
        int digits = Math.max(d, width);
        char zero = modifier == 'O' ? text.zeroDigit() : '0';

        int bufp = buf.length;
        boolean negative = value < 0;
        long u = negative ? -value : value;
        do {
            buf[--bufp] = (char) (zero + (int) (u % 10));
            u /= 10;
        } while (u != 0);

        boolean hasSign = negative || (signed && buf.length - bufp > plusMinimum);
        if (hasSign) {
            buf[--bufp] = negative ? '-' : '+';
        }
        emitNumber(bufp, digits, hasSign, zero);
    }

    /**
     * Pads and writes the digits already laid out in {@link #buf}, as glibc's
     * {@code do_number_sign_and_padding} label does. Zero padding goes after the sign, space
     * padding before it.
     */
    private void emitNumber(int bufp, int digits, boolean hasSign, char zero) {
        if (pad != '-') {
            int padding = digits - (buf.length - bufp);
            if (padding > 0) {
                if (pad == '_') {
                    appendRepeat(' ', padding);
                    width = width > padding ? width - padding : 0;
                } else {
                    if (hasSign) {
                        out.append(buf[bufp]);
                        bufp++;
                    }
                    appendRepeat(zero, padding);
                    width = 0;
                }
            }
        }
        int mark = out.length();
        out.append(buf, bufp, buf.length - bufp);
        padBefore(mark);
    }

    private void addChar(char c) {
        int mark = out.length();
        out.append(c);
        padBefore(mark);
    }

    private void copyString(String s) {
        int mark = out.length();
        out.append(s);
        padBefore(mark);
        applyCase(mark);
    }

    private void copySequence(CharSequence s, int from, int to) {
        int mark = out.length();
        out.append(s, from, to);
        padBefore(mark);
        applyCase(mark);
    }

    private void copyFormatted(DateTimeFormatter formatter, TemporalAccessor value) {
        int mark = out.length();
        formatter.formatTo(value, out);
        padBefore(mark);
        applyCase(mark);
    }

    /**
     * Left-pads whatever was written from {@code mark} onwards out to the field width, as glibc's
     * {@code add} macro does.
     */
    private void padBefore(int mark) {
        int delta = width - (out.length() - mark);
        if (delta <= 0) {
            return;
        }
        char[] filler = new char[delta];
        Arrays.fill(filler, pad == '0' ? '0' : ' ');
        out.insert(mark, filler);
    }

    private void applyCase(int mark) {
        if (!toUpper && !toLower) {
            return;
        }
        for (int i = mark; i < out.length(); i++) {
            char c = out.charAt(i);
            out.setCharAt(i, toLower ? Character.toLowerCase(c) : Character.toUpperCase(c));
        }
    }

    private void appendRepeat(char c, int count) {
        for (int i = 0; i < count; i++) {
            out.append(c);
        }
    }

    /**
     * Echoes the whole specifier, {@code '%'} included, which is the most useful reading of input
     * that may simply have been mis-sliced.
     *
     * @see <a href="https://sourceware.org/git/?p=glibc.git;a=blob;f=time/strftime_l.c;hb=HEAD#l1433">bad_format</a>
     */
    private void badFormat(CharSequence pattern, int specStart, int convPos) {
        copySequence(pattern, specStart, convPos + 1);
    }
}
