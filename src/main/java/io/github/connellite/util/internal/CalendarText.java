package io.github.connellite.util.internal;

import java.text.DateFormatSymbols;
import java.time.Month;
import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DecimalStyle;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The locale data {@link Strftime} needs, read once per locale and then reused.
 *
 * <p>This is the stand-in for glibc's {@code _NL_CURRENT (LC_TIME, ...)} lookups: day and month
 * names, the AM/PM markers, the alternative digits behind the {@code O} modifier, and the calendar
 * system behind the {@code E} modifier. Name arrays are stored indexed the way
 * {@link BrokenDownTime} counts, so a conversion is an array read rather than a formatter call.
 *
 * <p>Based on glibc:
 * <a href="https://sourceware.org/git/?p=glibc.git;a=blob;f=time/strftime_l.c;hb=HEAD#l740">the
 * {@code %a}, {@code %A}, {@code %b}, {@code %B} and {@code %p} cases</a>, which copy a string
 * straight out of the locale, and
 * <a href="https://sourceware.org/git/?p=glibc.git;a=blob;f=time/strftime_l.c;hb=HEAD#l953">nl_get_alt_digit</a>
 * for the {@code O} modifier.
 */
public final class CalendarText {

    private static final ConcurrentHashMap<Locale, CalendarText> CACHE = new ConcurrentHashMap<>();

    /**
     * One locale is used over and over in practice, so it is kept outside the map as well. A stale
     * read only costs a map lookup.
     */
    private static volatile CalendarText latest = of(Locale.ROOT);

    private final Locale locale;

    /** Indexed by {@link BrokenDownTime#dayOfWeek}, Sunday first. */
    private final String[] shortWeekdays;
    private final String[] fullWeekdays;

    /** Indexed by month minus one. */
    private final String[] shortMonths;
    private final String[] fullMonths;

    /** The stand-alone month forms, glibc's {@code a_altmonth} and {@code f_altmonth}. */
    private final String[] shortStandaloneMonths;
    private final String[] fullStandaloneMonths;

    private final String[] amPm;

    /**
     * The locale's zero digit. When it is ASCII {@code '0'} the locale has no alternative numeric
     * symbols, which is the case POSIX says must behave as if the modifier were absent.
     */
    private final char zeroDigit;

    /**
     * The locale's calendar system. {@link IsoChronology} means the locale declares no era data,
     * matching glibc's {@code era == NULL}, so every {@code E} conversion falls back.
     */
    private final Chronology chronology;
    private final boolean hasEras;

    private DateTimeFormatter dateFormat;
    private DateTimeFormatter timeFormat;
    private DateTimeFormatter zoneFormat;
    private DateTimeFormatter eraDateFormat;
    private DateTimeFormatter eraTimeFormat;
    private DateTimeFormatter eraDateTimeFormat;
    private DateTimeFormatter eraYearFormat;

    private CalendarText(Locale locale) {
        this.locale = locale;

        DateFormatSymbols symbols = DateFormatSymbols.getInstance(locale);
        this.shortWeekdays = weekdays(symbols.getShortWeekdays());
        this.fullWeekdays = weekdays(symbols.getWeekdays());
        this.shortMonths = months(symbols.getShortMonths());
        this.fullMonths = months(symbols.getMonths());
        this.shortStandaloneMonths = standaloneMonths(locale, TextStyle.SHORT_STANDALONE);
        this.fullStandaloneMonths = standaloneMonths(locale, TextStyle.FULL_STANDALONE);
        this.amPm = symbols.getAmPmStrings();

        this.zeroDigit = DecimalStyle.of(locale).getZeroDigit();
        this.chronology = Chronology.ofLocale(locale);
        this.hasEras = !IsoChronology.INSTANCE.equals(chronology);
    }

    public static CalendarText of(Locale locale) {
        CalendarText cached = latest;
        if (cached != null && cached.locale.equals(locale)) {
            return cached;
        }
        CalendarText text = CACHE.computeIfAbsent(locale, CalendarText::new);
        latest = text;
        return text;
    }

    /**
     * Re-indexes {@link DateFormatSymbols} weekday names, which start at {@link Calendar#SUNDAY}
     * and leave index zero empty, so that {@link BrokenDownTime#dayOfWeek} indexes them directly.
     */
    private static String[] weekdays(String[] symbols) {
        String[] names = new String[7];
        System.arraycopy(symbols, 1, names, 0, 7);
        return names;
    }

    /**
     * Copies the first twelve month names, dropping the thirteenth slot
     * {@link DateFormatSymbols} keeps for lunar calendars.
     */
    private static String[] months(String[] symbols) {
        String[] names = new String[12];
        System.arraycopy(symbols, 0, names, 0, 12);
        return names;
    }

    private static String[] standaloneMonths(Locale locale, TextStyle style) {
        String[] names = new String[12];
        for (Month month : Month.values()) {
            names[month.getValue() - 1] = month.getDisplayName(style, locale);
        }
        return names;
    }

    Locale locale() {
        return locale;
    }

    String shortWeekday(int dayOfWeek) {
        return shortWeekdays[dayOfWeek];
    }

    String fullWeekday(int dayOfWeek) {
        return fullWeekdays[dayOfWeek];
    }

    String shortMonth(int month, boolean standalone) {
        return standalone ? shortStandaloneMonths[month - 1] : shortMonths[month - 1];
    }

    String fullMonth(int month, boolean standalone) {
        return standalone ? fullStandaloneMonths[month - 1] : fullMonths[month - 1];
    }

    String amPm(int hour) {
        return amPm[hour < 12 ? 0 : 1];
    }

    char zeroDigit() {
        return zeroDigit;
    }

    boolean hasEras() {
        return hasEras;
    }

    Chronology chronology() {
        return chronology;
    }

    DateTimeFormatter dateFormat() {
        DateTimeFormatter existing = dateFormat;
        if (existing == null) {
            existing = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale);
            dateFormat = existing;
        }
        return existing;
    }

    DateTimeFormatter timeFormat() {
        DateTimeFormatter existing = timeFormat;
        if (existing == null) {
            existing = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).withLocale(locale);
            timeFormat = existing;
        }
        return existing;
    }

    DateTimeFormatter zoneFormat() {
        DateTimeFormatter existing = zoneFormat;
        if (existing == null) {
            existing = DateTimeFormatter.ofPattern("z", locale);
            zoneFormat = existing;
        }
        return existing;
    }

    DateTimeFormatter eraDateFormat() {
        DateTimeFormatter existing = eraDateFormat;
        if (existing == null) {
            existing = dateFormat().withChronology(chronology);
            eraDateFormat = existing;
        }
        return existing;
    }

    DateTimeFormatter eraTimeFormat() {
        DateTimeFormatter existing = eraTimeFormat;
        if (existing == null) {
            existing = timeFormat().withChronology(chronology);
            eraTimeFormat = existing;
        }
        return existing;
    }

    DateTimeFormatter eraDateTimeFormat() {
        DateTimeFormatter existing = eraDateTimeFormat;
        if (existing == null) {
            existing = DateTimeFormatter
                    .ofLocalizedDateTime(FormatStyle.MEDIUM)
                    .withLocale(locale)
                    .withChronology(chronology);
            eraDateTimeFormat = existing;
        }
        return existing;
    }

    /**
     * The era and its year together, glibc's {@code era_format}. The JDK carries no equivalent of
     * that locale field, so the CLDR era-plus-year pattern is used instead.
     */
    DateTimeFormatter eraYearFormat() {
        DateTimeFormatter existing = eraYearFormat;
        if (existing == null) {
            existing = DateTimeFormatter.ofPattern("GGGGy", locale).withChronology(chronology);
            eraYearFormat = existing;
        }
        return existing;
    }
}
