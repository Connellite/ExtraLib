package io.github.connellite.util.internal.dateparse;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Set;

/**
 * Single-pass scanner that recognises the layout of a date-time string while reading it, instead of
 * trying a list of patterns until one matches.
 *
 * <p>The scanner walks the input once, classifies every run of digits or letters, and assigns it to
 * a field. Nothing is allocated for the input itself (no {@code trim}, no {@code substring}) and no
 * exception is thrown when the input does not match: {@link #scan} simply returns {@code false}.
 *
 * <p>Recognised shapes include ISO and RFC 3339 date-times with offset or bracketed zone, SQL
 * timestamps, compact {@code yyyyMMdd[THHmmss]}, numeric dates with a two-digit year resolved
 * against a sliding window, localized month and weekday names, ordinal day suffixes, AM/PM markers
 * and zone abbreviations. Day-month order in all-numeric dates is decided by
 * {@link #orderDayAndMonth()}.
 *
 * <p>Based on araddon/dateparse:
 * <a href="https://github.com/araddon/dateparse/blob/5dd51ed0f76a3790e35b502070d9acbb404fab30/parseany.go#L146">ParseAny</a>
 * and its
 * <a href="https://github.com/araddon/dateparse/blob/5dd51ed0f76a3790e35b502070d9acbb404fab30/parseany.go#L266">iterRunes</a>
 * state machine, which records the start and length of every field while scanning and derives the
 * layout from them rather than trying patterns in turn. Digit-by-index reading of ISO fields also
 * follows Jackson's
 * <a href="https://github.com/FasterXML/jackson-databind/blob/de62c677b12c02967c9620a11c1c5208f701cfef/src/main/java/com/fasterxml/jackson/databind/util/ISO8601Utils.java#L115">ISO8601Utils.parse</a>
 * and pydantic/speedate's
 * <a href="https://github.com/pydantic/speedate/blob/57927b74423bba3a652ff6a2315a2527d4c90e0d/src/lib.rs#L30">get_digit!</a>.
 */
public final class DateTimeScanner {

    private static final int UNSET = Integer.MIN_VALUE;

    /**
     * Year, month and day markers of CJK dates, which separate fields instead of naming them.
     *
     * <p>Based on araddon/dateparse:
     * <a href="https://github.com/araddon/dateparse/blob/5dd51ed0f76a3790e35b502070d9acbb404fab30/parseany.go#L1931">dateDigitChineseYear</a>
     * ({@code 2006年01月02日}).
     */
    private static final String CJK_DATE_MARKERS = "\u5e74\u6708\u65e5";

    private final CharSequence text;
    private final CalendarNames names;
    private int pos;
    private int end;

    private int year = UNSET;
    private int month = UNSET;
    private int day = UNSET;
    private int hour = UNSET;
    private int minute;
    private int second;
    private int nano;
    private int amPm = CalendarNames.NOT_FOUND;
    private int offsetSeconds = UNSET;
    private ZoneId zone;

    private boolean monthFromName;
    private boolean provisionalOrder;
    private char datePunctuation;
    private boolean spaceSeparatedDate;
    private boolean previousWasDateField;
    private char pendingSeparator;

    private DateTimeScanner(CharSequence text, int start, int end, CalendarNames names) {
        this.text = text;
        this.pos = start;
        this.end = end;
        this.names = names;
    }

    /**
     * Scans {@code text} between {@code start} and {@code end} and fills {@code out}.
     *
     * @param names name tables to resolve month, weekday and AM/PM tokens with
     * @return {@code true} when the whole range was recognised; when {@code false} is returned the
     * contents of {@code out} are undefined and the caller must reset or discard it
     */
    public static boolean scan(
            CharSequence text, int start, int end, CalendarNames names, ParsedFields out) {
        return new DateTimeScanner(text, start, end, names).run(out);
    }

    /**
     * Cheap test for whether consulting {@link CalendarNames#global()} could pay off: the input has
     * to hold both a word that may be a month name and a number.
     */
    public static boolean mayContainLocalizedNames(CharSequence text, int start, int end) {
        int letters = 0;
        boolean hasWord = false;
        boolean hasDigit = false;
        for (int i = start; i < end; i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c)) {
                hasWord |= ++letters >= 3;
            } else {
                letters = 0;
                hasDigit |= isDigit(c);
            }
        }
        return hasWord && hasDigit;
    }

    private boolean run(ParsedFields out) {
        trim();
        if (pos >= end) {
            return false;
        }
        char first = text.charAt(pos);
        if (first == '+' || first == '-') {
            // a signed or expanded year such as "+12024-03-21"; left to the fallback formatters
            return false;
        }
        while (pos < end) {
            char c = text.charAt(pos);
            if (isDigit(c)) {
                if (!readNumber()) {
                    return false;
                }
            } else if (c == '+' || (c == '-' && hour != UNSET)) {
                if (!readOffset(c == '-' ? -1 : 1)) {
                    return false;
                }
            } else if (c == '[') {
                if (!readBracketedZone()) {
                    return false;
                }
            } else if (c == '\'') {
                if (!readApostropheYear()) {
                    return false;
                }
            } else if (c == '(') {
                if (!readTrailingZoneName()) {
                    return false;
                }
            } else if (isSeparator(c)) {
                recordSeparator(c);
                pos++;
            } else if (Character.isLetter(c)) {
                if (!readWord()) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return commit(out);
    }

    private void trim() {
        while (pos < end && isWhitespace(text.charAt(pos))) {
            pos++;
        }
        while (end > pos && isWhitespace(text.charAt(end - 1))) {
            end--;
        }
    }

    private void recordSeparator(char c) {
        if (c == ' ' || c == '\t' || c == ',') {
            if (pendingSeparator == 0) {
                pendingSeparator = ' ';
            }
        } else if (CJK_DATE_MARKERS.indexOf(c) >= 0) {
            // 年, 月 and 日 are one separator class, so 2024年04月08日 stays consistent
            pendingSeparator = CJK_DATE_MARKERS.charAt(0);
        } else {
            pendingSeparator = c;
        }
    }

    /**
     * Reads a run of digits and assigns it to the field its length and context imply.
     */
    private boolean readNumber() {
        int start = pos;
        while (pos < end && isDigit(text.charAt(pos))) {
            pos++;
        }
        int length = pos - start;

        if (pos < end && text.charAt(pos) == ':') {
            if (looksLikeColonDate(start, length)) {
                return readColonDate(start, length);
            }
            return length <= 2 && hour == UNSET && readTime(digitsAt(start, length));
        }
        if (year == UNSET && month == UNSET && day == UNSET
                && (length == 8 || length == 12 || length == 14)) {
            return readCompact(start, length);
        }
        if (length == 6 && hour == UNSET && day != UNSET) {
            hour = digitsAt(start, 2);
            minute = digitsAt(start + 2, 2);
            second = digitsAt(start + 4, 2);
            previousWasDateField = false;
            pendingSeparator = 0;
            return readFraction();
        }
        if (length == 6 && noDateYet() && hour == UNSET && followedByTime()) {
            return readCompactTwoDigitYearDate(start);
        }
        if (length == 4) {
            return year == UNSET && assignYear(digitsAt(start, 4));
        }
        if (length <= 2) {
            boolean ordinal = readOrdinalSuffix();
            if (!assignSmallNumber(digitsAt(start, length), length)) {
                return false;
            }
            if (ordinal) {
                // the suffix of "April 8th" names the day, so no order is left to guess
                provisionalOrder = false;
            }
            return true;
        }
        return false;
    }

    /**
     * Consumes an English ordinal suffix written directly after a day number.
     *
     * <p>Based on araddon/dateparse:
     * <a href="https://github.com/araddon/dateparse/blob/5dd51ed0f76a3790e35b502070d9acbb404fab30/parseany.go#L984">dateAlphaWsMonthSuffix</a>
     * ({@code April 8th, 2009}), which strips the suffix and parses the remainder.
     */
    private boolean readOrdinalSuffix() {
        if (pos + 1 >= end) {
            return false;
        }
        char first = Character.toLowerCase(text.charAt(pos));
        char second = Character.toLowerCase(text.charAt(pos + 1));
        boolean suffix = (first == 's' && second == 't')
                || (first == 'n' && second == 'd')
                || (first == 'r' && second == 'd')
                || (first == 't' && second == 'h');
        if (!suffix || (pos + 2 < end && Character.isLetter(text.charAt(pos + 2)))) {
            return false;
        }
        pos += 2;
        return true;
    }

    /**
     * Reads a compact {@code yyMMdd} date that is followed by a time.
     *
     * <p>Based on araddon/dateparse:
     * <a href="https://github.com/araddon/dateparse/blob/5dd51ed0f76a3790e35b502070d9acbb404fab30/parseany.go#L411">dateDigitSt</a>
     * ({@code 171113 14:14:20}). A bare run of six digits stays unrecognised there and here,
     * because it reads just as well as {@code HHmmss}.
     */
    private boolean readCompactTwoDigitYearDate(int start) {
        year = ParsedFields.expandTwoDigitYear(digitsAt(start, 2), LocalDate.now().getYear());
        month = digitsAt(start + 2, 2);
        day = digitsAt(start + 4, 2);
        previousWasDateField = true;
        pendingSeparator = 0;
        return true;
    }

    private boolean followedByTime() {
        int i = pos;
        while (i < end && isWhitespace(text.charAt(i))) {
            i++;
        }
        int digits = digitRunLength(i);
        return digits >= 1 && digits <= 2 && i + digits < end && text.charAt(i + digits) == ':';
    }

    /**
     * Decides whether a colon-separated run of numbers is a date rather than a time. Only a date
     * carries a four-digit field, so a leading or trailing one settles it; everything else, down to
     * {@code 1:2:06}, stays a time.
     *
     * <p>Based on araddon/dateparse:
     * <a href="https://github.com/araddon/dateparse/blob/5dd51ed0f76a3790e35b502070d9acbb404fab30/parseany.go#L618">dateDigitColon</a>
     * ({@code 2014:10:13}, {@code 10:13:2014}, {@code 01:02:2006}).
     */
    private boolean looksLikeColonDate(int start, int firstLength) {
        if (!noDateYet() || hour != UNSET) {
            return false;
        }
        if (firstLength == 4) {
            return true;
        }
        int i = start;
        int fields = 0;
        int lastLength = 0;
        while (i < end) {
            lastLength = digitRunLength(i);
            if (lastLength == 0) {
                return false;
            }
            fields++;
            i += lastLength;
            if (i >= end || text.charAt(i) != ':') {
                break;
            }
            i++;
        }
        return fields == 3 && lastLength == 4;
    }

    /**
     * Reads a date whose three fields are separated by colons, with {@link #pos} on the first colon.
     */
    private boolean readColonDate(int start, int firstLength) {
        if (!assignDateField(start, firstLength)) {
            return false;
        }
        for (int field = 0; field < 2; field++) {
            pos++;
            int length = digitRunLength();
            if (length < 1 || length > 4) {
                return false;
            }
            pendingSeparator = ':';
            if (!assignDateField(pos, length)) {
                return false;
            }
            pos += length;
            if (field == 0 && (pos >= end || text.charAt(pos) != ':')) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean assignDateField(int start, int length) {
        if (length == 4) {
            return year == UNSET && assignYear(digitsAt(start, 4));
        }
        return length <= 2 && assignSmallNumber(digitsAt(start, length), length);
    }

    private boolean noDateYet() {
        return year == UNSET && month == UNSET && day == UNSET;
    }

    /**
     * Handles {@code yyyyMMdd}, {@code yyyyMMddHHmm} and {@code yyyyMMddHHmmss}.
     *
     * <p>Based on Jackson:
     * <a href="https://github.com/FasterXML/jackson-databind/blob/de62c677b12c02967c9620a11c1c5208f701cfef/src/main/java/com/fasterxml/jackson/databind/util/ISO8601Utils.java#L115">ISO8601Utils.parse</a>,
     * which accepts the same compact {@code yyyyMMdd} / {@code hhmmss} forms without separators.
     */
    private boolean readCompact(int start, int length) {
        if (!assignYear(digitsAt(start, 4))) {
            return false;
        }
        month = digitsAt(start + 4, 2);
        day = digitsAt(start + 6, 2);
        if (length >= 12) {
            hour = digitsAt(start + 8, 2);
            minute = digitsAt(start + 10, 2);
        }
        if (length == 14) {
            second = digitsAt(start + 12, 2);
        }
        previousWasDateField = length == 8;
        pendingSeparator = 0;
        return length == 8 || readFraction();
    }

    private boolean assignYear(int value) {
        year = value;
        return markDateField();
    }

    /**
     * Assigns a one- or two-digit number. With a known year the next field is the month; otherwise
     * the leading number goes to the day and the order is settled later by {@link #orderDayAndMonth()},
     * and a trailing two-digit number is the year.
     */
    private boolean assignSmallNumber(int value, int length) {
        if (month == UNSET && (year != UNSET || day != UNSET)) {
            month = value;
        } else if (day == UNSET && (year == UNSET || month != UNSET)) {
            day = value;
            provisionalOrder = year == UNSET && month == UNSET;
        } else if (year == UNSET && length == 2) {
            year = ParsedFields.expandTwoDigitYear(value, LocalDate.now().getYear());
        } else {
            return false;
        }
        return markDateField();
    }

    /**
     * Rejects a date whose fields are glued together by inconsistent separators, and remembers when
     * they are only separated by spaces.
     */
    private boolean markDateField() {
        if (previousWasDateField) {
            if (pendingSeparator == ' ' || pendingSeparator == 0) {
                spaceSeparatedDate = true;
            } else if (datePunctuation == 0) {
                datePunctuation = pendingSeparator;
            } else if (datePunctuation != pendingSeparator) {
                return false;
            }
        }
        previousWasDateField = true;
        pendingSeparator = 0;
        return true;
    }

    /**
     * Reads {@code mm[:ss[.fraction]]} with {@link #pos} on the colon after the hour.
     */
    private boolean readTime(int hourValue) {
        hour = hourValue;
        pos++;
        int minuteLength = digitRunLength();
        if (minuteLength < 1 || minuteLength > 2) {
            return false;
        }
        minute = digitsAt(pos, minuteLength);
        pos += minuteLength;

        if (pos < end && text.charAt(pos) == ':') {
            pos++;
            int secondLength = digitRunLength();
            if (secondLength < 1 || secondLength > 2) {
                return false;
            }
            second = digitsAt(pos, secondLength);
            pos += secondLength;
            if (pos < end && text.charAt(pos) == ':' && !readColonFraction()) {
                return false;
            }
        }
        previousWasDateField = false;
        pendingSeparator = 0;
        return readFraction();
    }

    /**
     * Reads a fraction of a second that a fourth colon introduces instead of a period, as in
     * {@code 18:31:59:257}, with {@link #pos} on that colon.
     *
     * <p>Based on araddon/dateparse:
     * <a href="https://github.com/araddon/dateparse/blob/5dd51ed0f76a3790e35b502070d9acbb404fab30/parseany.go#L1256">timeStart</a>
     * ("ms uses colon"), which rewrites the colon into a period before handing the string to
     * {@code time.Parse}.
     */
    private boolean readColonFraction() {
        int digits = digitRunLength(pos + 1);
        if (digits == 0) {
            return false;
        }
        pos++;
        readFractionDigits(digits);
        return true;
    }

    /**
     * Reads an optional fractional second; digits beyond nanosecond precision are dropped.
     */
    private boolean readFraction() {
        if (pos >= end) {
            return true;
        }
        char c = text.charAt(pos);
        if (c != '.' && c != ',') {
            return true;
        }
        int digits = digitRunLength(pos + 1);
        if (digits == 0) {
            return true;
        }
        pos++;
        readFractionDigits(digits);
        return true;
    }

    private void readFractionDigits(int digits) {
        int scale = 100_000_000;
        for (int i = 0; i < digits; i++, pos++) {
            if (i < 9) {
                nano += (text.charAt(pos) - '0') * scale;
                scale /= 10;
            }
        }
    }

    /**
     * Reads a run of letters: a month name, a weekday to ignore, an AM/PM marker, a zone
     * abbreviation, or the {@code T} that separates date from time.
     */
    private boolean readWord() {
        int start = pos;
        while (pos < end && Character.isLetter(text.charAt(pos))) {
            pos++;
        }
        if (pos - start == 1) {
            char c = text.charAt(start);
            if (c == 'T' || c == 't') {
                pendingSeparator = 0;
                previousWasDateField = false;
                return true;
            }
            if ((c == 'm' || c == 'M') && readMonotonicReading()) {
                return true;
            }
        }
        if (month == UNSET) {
            int value = names.monthOf(text, start, pos);
            if (value != CalendarNames.NOT_FOUND) {
                month = value;
                monthFromName = true;
                skipOptionalDot();
                return markDateField();
            }
        }
        if (hour != UNSET && amPm == CalendarNames.NOT_FOUND) {
            int value = names.amPmOf(text, start, pos);
            if (value != CalendarNames.NOT_FOUND) {
                skipOptionalDot();
                amPm = value;
                return true;
            }
        }
        int zoneOffset = CalendarNames.zoneOffsetSecondsOf(text, start, pos);
        if (zoneOffset != CalendarNames.NOT_FOUND) {
            offsetSeconds = zoneOffset;
            previousWasDateField = false;
            pendingSeparator = 0;
            skipOptionalDot();
            return true;
        }
        if (names.isWeekday(text, start, pos)) {
            skipOptionalDot();
            pendingSeparator = 0;
            return true;
        }
        if (isTimePreposition(start, pos)) {
            pendingSeparator = 0;
            previousWasDateField = false;
            return true;
        }
        return false;
    }

    /**
     * Whether the range is the {@code at} of {@code September 17, 2012 at 5:00pm UTC-05}, which
     * only announces the time that follows.
     *
     * <p>Based on araddon/dateparse:
     * <a href="https://github.com/araddon/dateparse/blob/5dd51ed0f76a3790e35b502070d9acbb404fab30/parseany.go#L1213">dateAlphaWsAtTime</a>,
     * which skips the word and restarts the hour.
     */
    private boolean isTimePreposition(int start, int end) {
        return end - start == 2
                && (text.charAt(start) == 'a' || text.charAt(start) == 'A')
                && (text.charAt(start + 1) == 't' || text.charAt(start + 1) == 'T');
    }

    /**
     * Skips the monotonic clock reading that Go appends to a formatted instant, as in
     * {@code 22:32:14.009 +0300 MSK m=+0.000000001}, with {@link #pos} right after its {@code m}.
     *
     * <p>Based on araddon/dateparse:
     * <a href="https://github.com/araddon/dateparse/blob/5dd51ed0f76a3790e35b502070d9acbb404fab30/parseany.go#L1148">timeWsAlphaZoneOffsetWsExtra</a>
     * ({@code 22:18:00 +0000 UTC m=+0.000000001}).
     */
    private boolean readMonotonicReading() {
        if (hour == UNSET || pos + 2 >= end || text.charAt(pos) != '=') {
            return false;
        }
        char sign = text.charAt(pos + 1);
        if (sign != '+' && sign != '-') {
            return false;
        }
        for (int i = pos + 2; i < end; i++) {
            char c = text.charAt(i);
            if (!isDigit(c) && c != '.') {
                return false;
            }
        }
        pos = end;
        return true;
    }

    /**
     * Reads {@code +HH}, {@code +HHmm}, {@code +HHmmss}, {@code +HH:mm} or {@code +HH:mm:ss}.
     *
     * <p>Based on Jackson:
     * <a href="https://github.com/FasterXML/jackson-databind/blob/de62c677b12c02967c9620a11c1c5208f701cfef/src/main/java/com/fasterxml/jackson/databind/util/ISO8601Utils.java#L203">ISO8601Utils.parse</a>
     * (offset branch), which accepts both {@code +HHmm} and {@code +HH:mm}.
     */
    private boolean readOffset(int sign) {
        pos++;
        int digits = digitRunLength();
        int offset;
        if (digits == 4 || digits == 6) {
            offset = digitsAt(pos, 2) * 3600 + digitsAt(pos + 2, 2) * 60;
            if (digits == 6) {
                offset += digitsAt(pos + 4, 2);
            }
            pos += digits;
        } else if (digits == 1 || digits == 2) {
            offset = digitsAt(pos, digits) * 3600;
            pos += digits;
            if (pos < end && text.charAt(pos) == ':') {
                pos++;
                int minuteLength = digitRunLength();
                if (minuteLength < 1 || minuteLength > 2) {
                    return false;
                }
                offset += digitsAt(pos, minuteLength) * 60;
                pos += minuteLength;
                if (pos < end && text.charAt(pos) == ':' && digitRunLength(pos + 1) == 2) {
                    offset += digitsAt(pos + 1, 2);
                    pos += 3;
                }
            }
        } else {
            return false;
        }
        offsetSeconds = sign * offset;
        previousWasDateField = false;
        pendingSeparator = 0;
        return true;
    }

    /**
     * Reads a bracketed region id such as {@code [Europe/Moscow]}.
     */
    private boolean readBracketedZone() {
        int start = pos + 1;
        int close = -1;
        for (int i = start; i < end; i++) {
            if (text.charAt(i) == ']') {
                close = i;
                break;
            }
        }
        if (close < 0) {
            return false;
        }
        String id = text.subSequence(start, close).toString();
        if (!AvailableZones.IDS.contains(id)) {
            return false;
        }
        zone = ZoneId.of(id);
        pos = close + 1;
        previousWasDateField = false;
        pendingSeparator = 0;
        return true;
    }

    /**
     * Reads a two-digit year written with a leading apostrophe, as in {@code oct 7, '70}.
     *
     * <p>Based on araddon/dateparse:
     * <a href="https://github.com/araddon/dateparse/blob/5dd51ed0f76a3790e35b502070d9acbb404fab30/parseany.go#L929">dateAlphaWsDigitMoreWs</a>,
     * which moves the start of the year past the apostrophe.
     */
    private boolean readApostropheYear() {
        if (year != UNSET || digitRunLength(pos + 1) != 2) {
            return false;
        }
        pos++;
        year = ParsedFields.expandTwoDigitYear(digitsAt(pos, 2), LocalDate.now().getYear());
        pos += 2;
        return markDateField();
    }

    /**
     * Skips a parenthesised zone name that closes the input, as in {@code +0200 (CEST)} or the
     * {@code (GMT Daylight Time)} that JavaScript appends. The offset before it already carries the
     * information, so the name is read only to confirm nothing else follows.
     *
     * <p>Based on araddon/dateparse:
     * <a href="https://github.com/araddon/dateparse/blob/5dd51ed0f76a3790e35b502070d9acbb404fab30/parseany.go#L1667">timeWsAlphaZoneOffsetWsExtra</a>
     * ({@code Tue, 11 Jul 2017 16:28:13 +0200 (CEST)}), which drops the group as extra text.
     */
    private boolean readTrailingZoneName() {
        for (int i = pos + 1; i < end; i++) {
            char c = text.charAt(i);
            if (c == ')') {
                pos = i + 1;
                return pos == end;
            }
            if (!Character.isLetter(c) && !isWhitespace(c)) {
                return false;
            }
        }
        return false;
    }

    private boolean commit(ParsedFields out) {
        if (hour != UNSET) {
            int resolved = hour;
            if (amPm != CalendarNames.NOT_FOUND) {
                if (hour > 12) {
                    return false;
                }
                if (amPm == CalendarNames.PM && hour < 12) {
                    resolved = hour + 12;
                } else if (amPm == CalendarNames.AM && hour == 12) {
                    resolved = 0;
                }
            }
            if (!out.setTime(resolved, minute, second, nano)) {
                return false;
            }
        }
        if (year != UNSET && month != UNSET && day != UNSET) {
            if (spaceSeparatedDate && !monthFromName) {
                return false;
            }
            orderDayAndMonth();
            if (!out.setDate(year, month, day)) {
                return false;
            }
        } else if (year != UNSET && day == UNSET && !spaceSeparatedDate) {
            if (!completePartialDate(out)) {
                return false;
            }
        } else if (year != UNSET || month != UNSET || day != UNSET || hour == UNSET) {
            return false;
        }
        if (offsetSeconds != UNSET && !out.setOffsetSeconds(offsetSeconds)) {
            return false;
        }
        if (zone != null) {
            out.setZone(zone);
        }
        return true;
    }

    /**
     * Completes a date that names a year but no day, giving the month and the day their lowest
     * legal value: {@code 2024-03} is the first of March, and {@code 2014} and
     * {@code 15:04:05 2008} are the first of January.
     *
     * <p>Based on araddon/dateparse:
     * <a href="https://github.com/araddon/dateparse/blob/5dd51ed0f76a3790e35b502070d9acbb404fab30/parseany.go#L1770">dateDigit</a>
     * and
     * <a href="https://github.com/araddon/dateparse/blob/5dd51ed0f76a3790e35b502070d9acbb404fab30/parseany.go#L1789">dateYearDash</a>,
     * which hand {@code time.Parse} a layout naming only the fields that are present and so inherit
     * its rule that "elements omitted from the value are assumed to be zero or, when zero is
     * impossible, one".
     */
    private boolean completePartialDate(ParsedFields out) {
        return out.setDate(year, month == UNSET ? 1 : month, 1);
    }

    /**
     * Decides whether the two leading numbers of an all-numeric date were day-month or month-day.
     *
     * <p>A value above 12 can only be the day, which settles most inputs on its own. A genuinely
     * ambiguous pair follows the locale, except for dot-separated dates: no month-first locale
     * writes {@code 12.02.1999}.
     *
     * <p>Based on araddon/dateparse:
     * <a href="https://github.com/araddon/dateparse/blob/5dd51ed0f76a3790e35b502070d9acbb404fab30/parseany.go#L1996">PreferMonthFirst</a>
     * and
     * <a href="https://github.com/araddon/dateparse/blob/5dd51ed0f76a3790e35b502070d9acbb404fab30/parseany.go#L2004">RetryAmbiguousDateWithSwap</a>;
     * the preference comes from the locale here instead of being a fixed default.
     */
    private void orderDayAndMonth() {
        if (!provisionalOrder || monthFromName || day > 12) {
            return;
        }
        if (month > 12 || !(datePunctuation == '.' || FieldOrder.dayFirst())) {
            int swapped = day;
            day = month;
            month = swapped;
        }
    }

    private void skipOptionalDot() {
        if (pos < end && text.charAt(pos) == '.') {
            pos++;
        }
    }

    private int digitRunLength() {
        return digitRunLength(pos);
    }

    private int digitRunLength(int from) {
        int i = from;
        while (i < end && isDigit(text.charAt(i))) {
            i++;
        }
        return i - from;
    }

    /**
     * Reads {@code count} digits by index so no substring is allocated and no number parser is called.
     *
     * <p>Based on pydantic/speedate:
     * <a href="https://github.com/pydantic/speedate/blob/57927b74423bba3a652ff6a2315a2527d4c90e0d/src/lib.rs#L30">get_digit!</a>
     * and Jackson:
     * <a href="https://github.com/FasterXML/jackson-databind/blob/de62c677b12c02967c9620a11c1c5208f701cfef/src/main/java/com/fasterxml/jackson/databind/util/ISO8601Utils.java#L285">ISO8601Utils.parseInt</a>.
     */
    private int digitsAt(int from, int count) {
        int value = 0;
        for (int i = from, limit = from + count; i < limit; i++) {
            value = value * 10 + (text.charAt(i) - '0');
        }
        return value;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    private static boolean isSeparator(char c) {
        return c == ' ' || c == '\t' || c == ',' || c == '-' || c == '/' || c == '.' || c == '_'
                || CJK_DATE_MARKERS.indexOf(c) >= 0;
    }

    private static final class AvailableZones {
        private static final Set<String> IDS = ZoneId.getAvailableZoneIds();
    }

    /**
     * Whether the {@linkplain Locale.Category#FORMAT formatting locale} writes the day before the
     * month, taken from its short date pattern ({@code dd/MM/yy} for the UK, {@code M/d/yy} for the
     * US). The answer is cached for the current locale, so a changed default is picked up.
     */
    private static final class FieldOrder {

        private static Locale locale;
        private static boolean dayFirst;

        static synchronized boolean dayFirst() {
            Locale current = Locale.getDefault(Locale.Category.FORMAT);
            if (!current.equals(locale)) {
                dayFirst = resolve(current);
                locale = current;
            }
            return dayFirst;
        }

        private static boolean resolve(Locale locale) {
            String pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
                    FormatStyle.SHORT, null, IsoChronology.INSTANCE, locale);
            int day = pattern.indexOf('d');
            int month = pattern.indexOf('M');
            return day >= 0 && (month < 0 || day < month);
        }
    }
}
