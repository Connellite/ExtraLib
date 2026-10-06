package io.github.connellite.util.internal.dateparse;

import java.text.DateFormatSymbols;
import java.time.DayOfWeek;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Case-insensitive lookup tables for month names, weekday names, AM/PM markers and time-zone
 * abbreviations, used by {@link DateTimeScanner}.
 *
 * <p>Lookups take a range of a {@link CharSequence} and allocate nothing: keys are stored
 * pre-normalized (lower-cased, without a trailing {@code '.'}) in an open-addressed table and
 * compared character by character.
 *
 * <p>{@link #primary()} covers English, Russian and the default locale and is built eagerly.
 * {@link #global()} additionally covers one locale per language known to the JDK and is built on
 * first use, because that build walks a few hundred locales. Tokens that mean different months in
 * different languages are dropped from the global table unless the primary table defines them.
 *
 * <p>Based on sqlite-jdbc:
 * <a href="https://github.com/xerial/sqlite-jdbc/blob/8e999fe889463c1cd532c3e7bcbc64d107999eac/src/main/java/org/sqlite/date/FastDateParser.java#L414">FastDateParser.getDisplayNames</a>
 * and
 * <a href="https://github.com/xerial/sqlite-jdbc/blob/8e999fe889463c1cd532c3e7bcbc64d107999eac/src/main/java/org/sqlite/date/FastDateParser.java#L627">CaseInsensitiveTextStrategy</a>,
 * which collect every style a locale renders and lower-case the names once up front; the
 * alternation regex they build is replaced here by a table that can be probed without allocating.
 */
public final class CalendarNames {

    public static final int NOT_FOUND = -1;
    public static final int AM = 0;
    public static final int PM = 1;

    private static final int AMBIGUOUS = -2;

    private static final TextStyle[] STYLES = {
            TextStyle.FULL, TextStyle.FULL_STANDALONE, TextStyle.SHORT, TextStyle.SHORT_STANDALONE
    };

    private static final CalendarNames PRIMARY = buildPrimary();

    private static final NameTable ZONE_OFFSETS = buildZoneOffsets();

    private final NameTable months;
    private final NameTable weekdays;
    private final NameTable amPm;

    private CalendarNames(NameTable months, NameTable weekdays, NameTable amPm) {
        this.months = months;
        this.weekdays = weekdays;
        this.amPm = amPm;
    }

    /**
     * @return names of English, Russian and the default locale
     */
    public static CalendarNames primary() {
        return PRIMARY;
    }

    /**
     * @return {@linkplain #primary() primary} names plus one locale per JDK language; the first call
     * builds the tables
     */
    public static CalendarNames global() {
        return GlobalHolder.INSTANCE;
    }

    /**
     * @return month number 1-12, or {@link #NOT_FOUND}
     */
    public int monthOf(CharSequence text, int start, int end) {
        return months.get(text, start, end);
    }

    /**
     * @return {@code true} when the range names a day of the week
     */
    public boolean isWeekday(CharSequence text, int start, int end) {
        return weekdays.get(text, start, end) != NOT_FOUND;
    }

    /**
     * @return {@link #AM}, {@link #PM}, or {@link #NOT_FOUND}
     */
    public int amPmOf(CharSequence text, int start, int end) {
        return amPm.get(text, start, end);
    }

    /**
     * Resolves a time-zone abbreviation to a fixed offset.
     *
     * <p>Abbreviations are inherently ambiguous across regions ({@code CST} and {@code IST} name
     * several zones); the most widely used reading is applied, as other lenient parsers do.
     *
     * <p>Based on sqlite-jdbc:
     * <a href="https://github.com/xerial/sqlite-jdbc/blob/8e999fe889463c1cd532c3e7bcbc64d107999eac/src/main/java/org/sqlite/date/FastDateParser.java#L743">FastDateParser.TimeZoneStrategy</a>,
     * which resolves them from {@code DateFormatSymbols.getZoneStrings()} per locale instead.
     *
     * @return offset in seconds, or {@link #NOT_FOUND}
     */
    public static int zoneOffsetSecondsOf(CharSequence text, int start, int end) {
        return ZONE_OFFSETS.get(text, start, end);
    }

    private static final class GlobalHolder {
        private static final CalendarNames INSTANCE = buildGlobal();
    }

    private static CalendarNames buildPrimary() {
        Map<String, Integer> months = new HashMap<>();
        Map<String, Integer> weekdays = new HashMap<>();
        Map<String, Integer> amPm = new HashMap<>();
        for (Locale locale : primaryLocales()) {
            collect(locale, months, weekdays, amPm);
        }
        return new CalendarNames(
                new NameTable(months), new NameTable(weekdays), new NameTable(amPm));
    }

    private static CalendarNames buildGlobal() {
        Map<String, Integer> months = new HashMap<>();
        Map<String, Integer> weekdays = new HashMap<>();
        Map<String, Integer> amPm = new HashMap<>();

        for (String language : languages()) {
            collect(new Locale(language), months, weekdays, amPm);
        }
        months.values().removeIf(value -> value == AMBIGUOUS);
        weekdays.values().removeIf(value -> value == AMBIGUOUS);
        amPm.values().removeIf(value -> value == AMBIGUOUS);

        // primary languages win over anything a rarer language assigns to the same token
        for (Locale locale : primaryLocales()) {
            Map<String, Integer> localeMonths = new HashMap<>();
            Map<String, Integer> localeWeekdays = new HashMap<>();
            Map<String, Integer> localeAmPm = new HashMap<>();
            collect(locale, localeMonths, localeWeekdays, localeAmPm);
            override(months, localeMonths);
            override(weekdays, localeWeekdays);
            override(amPm, localeAmPm);
        }
        return new CalendarNames(
                new NameTable(months), new NameTable(weekdays), new NameTable(amPm));
    }

    private static Set<Locale> primaryLocales() {
        Set<Locale> locales = new LinkedHashSet<>();
        locales.add(Locale.ENGLISH);
        locales.add(new Locale("ru"));
        locales.add(Locale.getDefault());
        return locales;
    }

    private static Set<String> languages() {
        Set<String> languages = new HashSet<>();
        for (Locale locale : Locale.getAvailableLocales()) {
            String language = locale.getLanguage();
            if (!language.isEmpty()) {
                languages.add(language);
            }
        }
        return languages;
    }

    private static void collect(
            Locale locale,
            Map<String, Integer> months,
            Map<String, Integer> weekdays,
            Map<String, Integer> amPm) {

        Map<String, Integer> localeMonths = new HashMap<>();
        for (Month month : Month.values()) {
            int number = month.getValue();
            for (TextStyle style : STYLES) {
                String name = month.getDisplayName(style, locale);
                put(localeMonths, name, number);
                // "Sept" and "янв" are common renderings the CLDR styles above do not produce
                put(localeMonths, prefix(name, 3), number);
                put(localeMonths, prefix(name, 4), number);
            }
        }
        merge(months, localeMonths);

        Map<String, Integer> localeWeekdays = new HashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            for (TextStyle style : STYLES) {
                String name = day.getDisplayName(style, locale);
                put(localeWeekdays, name, day.getValue());
                put(localeWeekdays, prefix(name, 3), day.getValue());
            }
        }
        merge(weekdays, localeWeekdays);

        String[] markers = DateFormatSymbols.getInstance(locale).getAmPmStrings();
        if (markers.length >= 2) {
            Map<String, Integer> localeAmPm = new HashMap<>();
            put(localeAmPm, markers[0], AM);
            put(localeAmPm, markers[1], PM);
            merge(amPm, localeAmPm);
        }
    }

    /**
     * Adds {@code name} unless another value in the same locale already claims that key; a token
     * that is ambiguous inside one locale is useless everywhere.
     */
    private static void put(Map<String, Integer> target, String name, int value) {
        String key = normalize(name);
        if (key == null) {
            return;
        }
        Integer previous = target.putIfAbsent(key, value);
        if (previous != null && previous != value) {
            target.put(key, AMBIGUOUS);
        }
    }

    private static void merge(Map<String, Integer> target, Map<String, Integer> source) {
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            if (entry.getValue() == AMBIGUOUS) {
                continue;
            }
            Integer previous = target.putIfAbsent(entry.getKey(), entry.getValue());
            if (previous != null && !previous.equals(entry.getValue())) {
                target.put(entry.getKey(), AMBIGUOUS);
            }
        }
    }

    private static void override(Map<String, Integer> target, Map<String, Integer> source) {
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            if (entry.getValue() != AMBIGUOUS) {
                target.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private static String prefix(String name, int length) {
        return name.length() > length ? name.substring(0, length) : null;
    }

    /**
     * Lower-cases per character and drops a trailing {@code '.'}, so {@code "Мар."} and {@code "мар"}
     * share one key. Names containing digits are rejected: some locales render months numerically.
     */
    private static String normalize(String name) {
        if (name == null) {
            return null;
        }
        int end = name.length();
        if (end > 0 && name.charAt(end - 1) == '.') {
            end--;
        }
        if (end == 0) {
            return null;
        }
        char[] chars = new char[end];
        for (int i = 0; i < end; i++) {
            char c = name.charAt(i);
            if (c >= '0' && c <= '9') {
                return null;
            }
            chars[i] = Character.toLowerCase(c);
        }
        return new String(chars);
    }

    private static NameTable buildZoneOffsets() {
        Map<String, Integer> offsets = new HashMap<>();
        offsets.put("z", 0);
        offsets.put("ut", 0);
        offsets.put("utc", 0);
        offsets.put("uct", 0);
        offsets.put("gmt", 0);
        offsets.put("zulu", 0);
        offsets.put("universal", 0);
        offsets.put("wet", 0);
        offsets.put("west", 3600);
        offsets.put("cet", 3600);
        offsets.put("bst", 3600);
        offsets.put("cest", 2 * 3600);
        offsets.put("eet", 2 * 3600);
        offsets.put("sast", 2 * 3600);
        offsets.put("eest", 3 * 3600);
        offsets.put("msk", 3 * 3600);
        offsets.put("msd", 4 * 3600);
        offsets.put("ist", 5 * 3600 + 1800);
        offsets.put("ict", 7 * 3600);
        offsets.put("cst", -6 * 3600);
        offsets.put("jst", 9 * 3600);
        offsets.put("kst", 9 * 3600);
        offsets.put("aest", 10 * 3600);
        offsets.put("aedt", 11 * 3600);
        offsets.put("nzst", 12 * 3600);
        offsets.put("nzdt", 13 * 3600);
        offsets.put("est", -5 * 3600);
        offsets.put("edt", -4 * 3600);
        offsets.put("cdt", -5 * 3600);
        offsets.put("mst", -7 * 3600);
        offsets.put("mdt", -6 * 3600);
        offsets.put("pst", -8 * 3600);
        offsets.put("pdt", -7 * 3600);
        offsets.put("akst", -9 * 3600);
        offsets.put("akdt", -8 * 3600);
        offsets.put("hst", -10 * 3600);
        return new NameTable(offsets);
    }

    /**
     * Open-addressed {@code String -> int} table that can be probed with a {@link CharSequence}
     * range instead of a {@link String}.
     */
    private static final class NameTable {

        private final String[] keys;
        private final int[] values;
        private final int mask;

        NameTable(Map<String, Integer> entries) {
            int capacity = Integer.highestOneBit(Math.max(entries.size(), 1)) * 4;
            this.keys = new String[capacity];
            this.values = new int[capacity];
            this.mask = capacity - 1;
            for (Map.Entry<String, Integer> entry : entries.entrySet()) {
                if (entry.getValue() == AMBIGUOUS) {
                    continue;
                }
                insert(entry.getKey(), entry.getValue());
            }
        }

        private void insert(String key, int value) {
            int index = hash(key, 0, key.length()) & mask;
            while (keys[index] != null) {
                if (keys[index].equals(key)) {
                    values[index] = value;
                    return;
                }
                index = (index + 1) & mask;
            }
            keys[index] = key;
            values[index] = value;
        }

        int get(CharSequence text, int start, int end) {
            if (end > start && text.charAt(end - 1) == '.') {
                end--;
            }
            if (end <= start) {
                return NOT_FOUND;
            }
            int index = hash(text, start, end) & mask;
            while (true) {
                String key = keys[index];
                if (key == null) {
                    return NOT_FOUND;
                }
                if (matches(key, text, start, end)) {
                    return values[index];
                }
                index = (index + 1) & mask;
            }
        }

        private static int hash(CharSequence text, int start, int end) {
            int hash = 0;
            for (int i = start; i < end; i++) {
                hash = hash * 31 + Character.toLowerCase(text.charAt(i));
            }
            return hash ^ (hash >>> 16);
        }

        private static boolean matches(String key, CharSequence text, int start, int end) {
            if (key.length() != end - start) {
                return false;
            }
            for (int i = 0; i < key.length(); i++) {
                if (key.charAt(i) != Character.toLowerCase(text.charAt(start + i))) {
                    return false;
                }
            }
            return true;
        }
    }
}
