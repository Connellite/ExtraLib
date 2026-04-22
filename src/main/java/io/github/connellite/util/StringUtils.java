package io.github.connellite.util;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * String helpers
 */
@UtilityClass
public class StringUtils {

    /**
     * Returns a readable string for {@code value}, including array contents.
     *
     * <p>{@code null} becomes the literal {@code "null"}. Non-array values use {@link String#valueOf(Object)}.
     * Primitive arrays use the matching {@code Arrays.toString} overload; reference-type arrays use
     * {@link Arrays#deepToString(Object[])} so nested arrays are expanded instead of default object references like
     * {@code [I@…}.
     *
     * @param value any object or array, or {@code null}
     * @return text suitable for logging or default formatting
     */
    public static String toString(Object value) {
        if (value == null) {
            return "null";
        }
        Class<?> cl = value.getClass();
        if (!cl.isArray()) {
            return String.valueOf(value);
        }
        Class<?> comp = cl.getComponentType();
        if (!comp.isPrimitive()) {
            return Arrays.deepToString((Object[]) value);
        }
        if (comp == byte.class) {
            return Arrays.toString((byte[]) value);
        }
        if (comp == short.class) {
            return Arrays.toString((short[]) value);
        }
        if (comp == int.class) {
            return Arrays.toString((int[]) value);
        }
        if (comp == long.class) {
            return Arrays.toString((long[]) value);
        }
        if (comp == char.class) {
            return Arrays.toString((char[]) value);
        }
        if (comp == float.class) {
            return Arrays.toString((float[]) value);
        }
        if (comp == double.class) {
            return Arrays.toString((double[]) value);
        }
        if (comp == boolean.class) {
            return Arrays.toString((boolean[]) value);
        }
        return String.valueOf(value);
    }

    /**
     * Joins elements with {@link String#valueOf(Object)} for non-null items; {@code null} elements
     * are represented as the four characters {@code null}, same as {@code String.join(separator, elements)}
     * for an {@link Iterable} of {@link CharSequence}.
     *
     * @param items     sequence to join; not null (may be empty)
     * @param separator placed between elements; not null (may be empty)
     */
    public static <T> String join(@NonNull Iterable<T> items, @NonNull String separator) {
        StringJoiner joiner = new StringJoiner(separator);
        for (T item : items) {
            joiner.add(String.valueOf(item));
        }
        return joiner.toString();
    }

    /**
     * Joins elements using {@code toString} for non-null items; {@code null} elements are not passed
     * to {@code toString} — they are joined like {@link String#join(CharSequence, Iterable)} (the literal
     * {@code null}). If {@code toString} returns {@code null}, that is passed to {@link StringJoiner#add(CharSequence)}
     * and becomes the same four characters.
     *
     * @param items     sequence to join; not null (may be empty)
     * @param separator placed between elements; not null (may be empty)
     * @param toString  converts each non-null element; not null
     */
    public static <T> String join(
            @NonNull Iterable<T> items,
            @NonNull String separator,
            @NonNull Function<? super T, ? extends CharSequence> toString) {
        StringJoiner joiner = new StringJoiner(separator);
        for (T item : items) {
            joiner.add(item == null ? null : toString.apply(item));
        }
        return joiner.toString();
    }

    /**
     * Joins multiple values into one string, dispatching on the runtime shape of {@code obj}.
     *
     * <p>If {@code obj} is an {@link Iterable}, each element is appended with {@link String#valueOf(Object)}.
     * If it is any array (including primitives), elements are indexed with {@link Array#get}.
     * If it is an {@link Iterator}, remaining elements are consumed and appended (the iterator is advanced).
     * If it is a {@link Map}, each {@link Map.Entry} becomes {@code key=value} via string concatenation.
     * Otherwise a single segment {@link String#valueOf(Object)} is used.
     *
     * @param obj       non-null container, array, iterator, map, or scalar
     * @param separator placed between segments; not null (may be empty)
     */
    public static String join(@NonNull Object obj, @NonNull String separator) {
        StringJoiner joiner = new StringJoiner(separator);
        if (obj instanceof Iterable) {
            for (Object item : (Iterable<?>) obj) {
                joiner.add(String.valueOf(item));
            }
        } else if (obj.getClass().isArray()) {
            int length = Array.getLength(obj);
            for (int i = 0; i < length; i++) {
                joiner.add(String.valueOf(Array.get(obj, i)));
            }
        } else if (obj instanceof Iterator<?> iterator) {
            while (iterator.hasNext()) {
                joiner.add(String.valueOf(iterator.next()));
            }
        } else if (obj instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                joiner.add(entry.getKey() + "=" + entry.getValue());
            }
        } else {
            joiner.add(String.valueOf(obj));
        }
        return joiner.toString();
    }

    /**
     * Splits {@code input} on line breaks using {@code \n} or {@code \r\n} as separators
     * ({@link String#split(String)} with {@code \r?\n}).
     *
     * @param input the text; must not be {@code null}
     * @return a list of lines (may contain a single element for text without newlines; behavior matches {@link String#split})
     * @throws NullPointerException if {@code input} is {@code null}
     */
    public static List<String> splitLines(String input) {
        return Arrays.asList(input.split("\\r?\\n"));
    }

    /**
     * Splits {@code string} with {@link String#split(String, int)} using {@code limit} {@code 0}
     * (trailing empty segments discarded per {@link String#split(String)}), trims each segment with
     * {@link String#trim()}, and omits segments that are {@linkplain String#isBlank() blank} after trimming.
     *
     * @param string text to split; may be {@code null} (returns an empty list)
     * @param regex  delimiter pattern passed to {@link String#split(String, int)}; must not be {@code null}
     * @return a new list of non-blank trimmed parts (never {@code null}; may be empty)
     * @throws NullPointerException if {@code regex} is {@code null}
     * @see String#split(String, int)
     */
    public static List<String> splitAndTrim(String string, String regex) {
        return splitAndTrim(string, regex, 0);
    }

    /**
     * Same as {@link #splitAndTrim(String, String)} but forwards {@code limit} to {@link String#split(String, int)}
     * so callers can cap splits or preserve trailing empties when {@code limit} is negative or greater than zero,
     * per {@link String#split(String, int)}.
     *
     * @param string text to split; may be {@code null} (returns an empty list)
     * @param regex  delimiter pattern; must not be {@code null}
     * @param limit  maximum number of parts; same semantics as {@link String#split(String, int)}
     * @return a new list of non-blank trimmed parts (never {@code null}; may be empty)
     * @throws NullPointerException if {@code regex} is {@code null}
     * @see String#split(String, int)
     */
    public static List<String> splitAndTrim(String string, @NonNull String regex, int limit) {
        if (string == null) {
            return Collections.emptyList();
        }

        String[] array = string.split(regex, limit);
        List<String> result = new ArrayList<>(array.length);
        for (String item : array) {
            item = item.trim();
            if (!item.isEmpty()) {
                result.add(item);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Normalizes line breaks in {@code text} for a single-line or flowed paragraph style.
     * <p>Applied in order:</p>
     * <ol>
     *   <li>Strips leading spaces and tabs on each line (after a newline or at the start) and
     *       trailing spaces and tabs before a newline or end of string.</li>
     *   <li>Removes a newline immediately after a hyphen-like character (ASCII hyphen, Unicode dashes,
     *       or soft hyphen U+00AD), keeping the hyphen (typical line-break hyphenation in plain text).</li>
     *   <li>Replaces remaining newlines that sit between two non-newline characters with a single space
     *       (joins wrapped lines). Newlines adjacent to other line breaks are left as-is because the
     *       lookahead/lookbehind use {@code .}, which does not match line terminators.</li>
     * </ol>
     * <p>If {@code removeSoftHyphens} is {@code true}, all soft hyphens (U+00AD) are removed from the result.</p>
     *
     * @param text              source text; must not be {@code null}
     * @param removeSoftHyphens when {@code true}, strips every U+00AD from the result
     * @return normalized text with the rules above applied
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public static String removeLineBreaks(@NonNull String text, boolean removeSoftHyphens) {
        text = text.replaceAll("(?<=\n|^)[\t ]+|[\t ]+(?=$|\n)", "");
        text = text.replaceAll("(?<=.)([-‐‑‒–—―\u00AD])\n(?=.)", "$1");
        text = text.replaceAll("(?<=.)\n(?=.)", " ");
        if (removeSoftHyphens) {
            text = text.replaceAll("\u00AD", "");
        }
        return text;
    }

    /**
     * Returns whether {@code input} looks like a signed decimal integer: an optional leading ASCII
     * hyphen ({@code '-'}) followed only by characters for which {@link Character#isDigit(char)} is
     * {@code true}.
     * <p>{@code null} and the empty sequence return {@code false}. A lone {@code "-"} is not numeric
     * (there must be at least one digit after an optional sign).</p>
     *
     * @param input text to inspect; may be {@code null}
     * @return {@code true} if non-empty and every character passes the rules above
     */
    public static boolean isNumeric(final CharSequence input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        int sz = input.length();

        for (int i = 0; i < sz; ++i) {
            char currentChar = input.charAt(i);
            if (currentChar == '-' && i == 0 && sz > 1)
                continue;
            if (!Character.isDigit(currentChar)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Builds a {@link Pattern} from a simple match string with shell-style wildcards, then compiles it
     * with {@link Pattern#CASE_INSENSITIVE} and {@link Pattern#DOTALL}.
     * <p>Transformations apply only to characters that are not escaped by a preceding backslash
     * (per-step {@code (?<!\\\\)} rules in the implementation):</p>
     * <ul>
     *   <li>Runs of paired backslashes are replaced by a single {@code *}.</li>
     *   <li>Redundant unescaped {@code ?} / {@code *} clusters (e.g. {@code ?**}) collapse to one {@code *}.</li>
     *   <li>Remaining regex metacharacters in {@code |[]{}(),.^$+-} are escaped for literal matching.</li>
     *   <li>Unescaped {@code ?} becomes {@code (.?)} (zero or one of any character, including newlines).</li>
     *   <li>Unescaped {@code *} becomes {@code (.*)} (any length, including newlines).</li>
     * </ul>
     *
     * @param match user-facing pattern; must not be {@code null}
     * @return compiled pattern ready for {@link Matcher#matches()}, {@link Matcher#find()}, etc.
     * @throws NullPointerException   if {@code match} is {@code null}
     * @throws PatternSyntaxException if the transformed string is not a valid regex
     */
    public static Pattern compileMatchPattern(String match) {
        // replace duplicate stars
        // match = match.replaceAll("(\\*)\\1+", "*");

        // replace any pair of backslashes by [*]
        match = match.replaceAll("(?<!\\\\)(\\\\\\\\)+(?!\\\\)", "*");
        // minimize unescaped redundant wildcards
        match = match.replaceAll("(?<!\\\\)[?]*[*][*?]+", "*");
        // escape unescaped regexps special chars, but [\], [?] and [*]
        match = match.replaceAll("(?<!\\\\)([|\\[\\]{}(),.^$+-])", "\\\\$1");
        // replace unescaped [?] by [.?]
        match = match.replaceAll("(?<!\\\\)[?]", "(.?)");
        // replace unescaped [*] by [.*]
        match = match.replaceAll("(?<!\\\\)[*]", "(.*)");
        return Pattern.compile(match, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    }

    /**
     * Returns whether {@code input} equals its {@link String#toUpperCase()} form using the default locale.
     *
     * @param input the string; must not be {@code null}
     * @return {@code true} if unchanged by {@code toUpperCase()}
     * @throws NullPointerException if {@code input} is {@code null}
     */
    public static boolean isUpperCase(String input) {
        return Objects.equals(input, input.toUpperCase());
    }

    /**
     * Returns whether {@code input} equals its {@link String#toLowerCase()} form using the default locale.
     *
     * @param input the string; must not be {@code null}
     * @return {@code true} if unchanged by {@code toLowerCase()}
     * @throws NullPointerException if {@code input} is {@code null}
     */
    public static boolean isLowerCase(String input) {
        return Objects.equals(input, input.toLowerCase());
    }

    /**
     * Reads all bytes from {@code in} until end-of-stream and decodes them as UTF-8.
     * <p>Does not close {@code in}.</p>
     *
     * @param in the stream; must not be {@code null}
     * @return the full decoded string
     * @throws IOException if reading fails
     */
    public static String convertInputStreamToString(final InputStream in) throws IOException {
        return convertInputStreamToString(in, StandardCharsets.UTF_8);
    }

    /**
     * Reads all bytes from {@code in} until end-of-stream and decodes them with {@code charset}.
     * <p>Does not close {@code in}.</p>
     *
     * @param in      the stream; must not be {@code null}
     * @param charset character set for decoding; must not be {@code null}
     * @return the full decoded string
     * @throws IOException if reading fails
     */
    public static String convertInputStreamToString(final InputStream in, Charset charset) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(charset);
    }

    /**
     * Token pattern for splitting mixed {@code camelCase}, {@code PascalCase}, acronyms, and digit runs
     * before {@link #toCamelCase}, {@link #toKebabCase}, and {@link #toSnakeCase}.
     */
    private static final Pattern CASE_CONVENTION_TOKEN_PATTERN = Pattern.compile(
            "[A-Z]{2,}(?=[A-Z][a-z]+[0-9]*|\\b)|[A-Z]?[a-z]+[0-9]*|[A-Z]|[0-9]+");

    /**
     * Converts a phrase or identifier to lower {@code camelCase}: first token lowercased, rest title-cased and concatenated.
     * <p>Uses {@link #CASE_CONVENTION_TOKEN_PATTERN} to segment the input.</p>
     *
     * @param input the source string; must not be {@code null}
     * @return camel-cased text
     * @throws NullPointerException            if {@code input} is {@code null}
     * @throws StringIndexOutOfBoundsException if no tokens match (e.g. {@code input} is empty)
     */
    public static String toCamelCase(String input) {
        Matcher matcher = CASE_CONVENTION_TOKEN_PATTERN.matcher(input);
        List<String> matchedParts = new ArrayList<>();
        while (matcher.find()) {
            matchedParts.add(matcher.group(0));
        }
        String s = matchedParts.stream()
                .map(x -> x.substring(0, 1).toUpperCase() + x.substring(1).toLowerCase())
                .collect(Collectors.joining());
        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }

    /**
     * Converts a phrase or identifier to {@code kebab-case}: tokens lowercased and joined with {@code '-'}.
     * <p>Uses {@link #CASE_CONVENTION_TOKEN_PATTERN} to segment the input.</p>
     *
     * @param input the source string; must not be {@code null}
     * @return kebab-cased text (empty if {@code input} is empty)
     * @throws NullPointerException if {@code input} is {@code null}
     */
    public static String toKebabCase(String input) {
        Matcher matcher = CASE_CONVENTION_TOKEN_PATTERN.matcher(input);
        List<String> matchedParts = new ArrayList<>();
        while (matcher.find()) {
            matchedParts.add(matcher.group(0));
        }
        return matchedParts.stream()
                .map(String::toLowerCase)
                .collect(Collectors.joining("-"));
    }

    /**
     * Converts a phrase or identifier to {@code snake_case}: tokens lowercased and joined with {@code '_'}.
     * <p>Uses {@link #CASE_CONVENTION_TOKEN_PATTERN} to segment the input.</p>
     *
     * @param input the source string; must not be {@code null}
     * @return snake-cased text (empty if {@code input} is empty)
     * @throws NullPointerException if {@code input} is {@code null}
     */
    public static String toSnakeCase(String input) {
        Matcher matcher = CASE_CONVENTION_TOKEN_PATTERN.matcher(input);
        List<String> matchedParts = new ArrayList<>();
        while (matcher.find()) {
            matchedParts.add(matcher.group(0));
        }
        return matchedParts.stream()
                .map(String::toLowerCase)
                .collect(Collectors.joining("_"));
    }
}
