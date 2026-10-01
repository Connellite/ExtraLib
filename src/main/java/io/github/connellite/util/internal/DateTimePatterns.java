package io.github.connellite.util.internal;

import lombok.experimental.UtilityClass;

import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Locale;

/**
 * Safety net for inputs {@link DateTimeScanner} does not recognise, such as signed or expanded
 * years.
 *
 * <p>Formatters are probed with {@link DateTimeFormatter#parseUnresolved(CharSequence, ParsePosition)},
 * which reports a mismatch by returning {@code null} instead of throwing, so a failing input costs
 * no exceptions. Resolution and range validation are done by {@link ParsedFields}, because
 * {@code parseUnresolved} deliberately skips both.
 *
 * <p>This is the last remaining piece of the previous shotgun approach that walked a list of
 * {@link DateTimeFormatter}s (comparable to what araddon/dateparse explicitly avoids — see
 * <a href="https://github.com/araddon/dateparse/blob/5dd51ed0f76a3790e35b502070d9acbb404fab30/parseany.go#L146">ParseAny</a>).
 * It only runs when the scanner returns {@code false}.
 */
@UtilityClass
public class DateTimePatterns {

    private static final DateTimeFormatter[] FALLBACK = {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.RFC_1123_DATE_TIME,

            new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd HH:mm:ss")
                    .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                    .toFormatter(),

            new DateTimeFormatterBuilder()
                    .appendPattern("yyyy/MM/dd HH:mm:ss")
                    .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                    .toFormatter(),

            new DateTimeFormatterBuilder()
                    .appendPattern("dd.MM.yyyy HH:mm:ss")
                    .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                    .toFormatter(),

            new DateTimeFormatterBuilder()
                    .appendPattern("dd/MM/yyyy HH:mm:ss")
                    .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                    .toFormatter(),

            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ISO_DATE,
            DateTimeFormatter.BASIC_ISO_DATE,
            DateTimeFormatter.ISO_LOCAL_TIME,
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),

            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("dd MMM yyyy")
                    .toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("yyyy MMM dd")
                    .toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("dd MMM yyyy")
                    .toFormatter(new Locale("ru")),
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("dd MMMM yyyy")
                    .toFormatter(new Locale("ru"))
    };

    /**
     * Tries every fallback formatter against {@code text} between {@code start} and {@code end}.
     *
     * @return {@code true} when one of them consumed the whole range and produced usable fields;
     * when {@code false} is returned the contents of {@code out} are undefined
     */
    public static boolean parse(CharSequence text, int start, int end, ParsedFields out) {
        CharSequence input = start == 0 && end == text.length()
                ? text
                : text.subSequence(start, end);

        ParsePosition position = new ParsePosition(0);
        for (DateTimeFormatter formatter : FALLBACK) {
            position.setIndex(0);
            position.setErrorIndex(-1);

            TemporalAccessor parsed;
            try {
                parsed = formatter.parseUnresolved(input, position);
            } catch (DateTimeException e) {
                // a few parsers range-check eagerly, for example an offset of "+25:00"
                continue;
            }
            if (parsed == null || position.getIndex() != input.length()) {
                continue;
            }
            if (fill(parsed, out)) {
                return true;
            }
            out.reset();
        }
        return false;
    }

    private static boolean fill(TemporalAccessor parsed, ParsedFields out) {
        int year = year(parsed);
        boolean hasDate = year != Integer.MIN_VALUE
                && parsed.isSupported(ChronoField.MONTH_OF_YEAR)
                && parsed.isSupported(ChronoField.DAY_OF_MONTH);
        if (hasDate && !out.setDate(
                year,
                field(parsed, ChronoField.MONTH_OF_YEAR),
                field(parsed, ChronoField.DAY_OF_MONTH))) {
            return false;
        }

        if (parsed.isSupported(ChronoField.HOUR_OF_DAY)) {
            if (!out.setTime(
                    field(parsed, ChronoField.HOUR_OF_DAY),
                    field(parsed, ChronoField.MINUTE_OF_HOUR),
                    field(parsed, ChronoField.SECOND_OF_MINUTE),
                    field(parsed, ChronoField.NANO_OF_SECOND))) {
                return false;
            }
        } else if (!hasDate) {
            return false;
        }

        if (parsed.isSupported(ChronoField.OFFSET_SECONDS)
                && !out.setOffsetSeconds(field(parsed, ChronoField.OFFSET_SECONDS))) {
            return false;
        }
        ZoneId zone = parsed.query(TemporalQueries.zoneId());
        if (zone != null) {
            out.setZone(zone);
        }
        return true;
    }

    /**
     * @return the proleptic year, or {@link Integer#MIN_VALUE} when the input carried no year;
     * {@code yyyy} patterns parse into {@link ChronoField#YEAR_OF_ERA}, which needs the era applied
     */
    private static int year(TemporalAccessor parsed) {
        if (parsed.isSupported(ChronoField.YEAR)) {
            return field(parsed, ChronoField.YEAR);
        }
        if (!parsed.isSupported(ChronoField.YEAR_OF_ERA)) {
            return Integer.MIN_VALUE;
        }
        int yearOfEra = field(parsed, ChronoField.YEAR_OF_ERA);
        boolean beforeCommonEra = parsed.isSupported(ChronoField.ERA)
                && field(parsed, ChronoField.ERA) == 0;
        return beforeCommonEra ? 1 - yearOfEra : yearOfEra;
    }

    private static int field(TemporalAccessor parsed, ChronoField field) {
        if (!parsed.isSupported(field)) {
            return 0;
        }
        long value = parsed.getLong(field);
        return value < Integer.MIN_VALUE || value > Integer.MAX_VALUE ? 0 : (int) value;
    }
}
