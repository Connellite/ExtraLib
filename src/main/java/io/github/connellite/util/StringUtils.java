package io.github.connellite.util;

import lombok.experimental.UtilityClass;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * String helpers
 */
@UtilityClass
public class StringUtils {

    /**
     * Joins elements with {@link String#valueOf(Object)} for non-null items; {@code null} elements
     * are represented as the four characters {@code null}, same as {@code String.join(separator, elements)}
     * for an {@link Iterable} of {@link CharSequence}.
     *
     * @param items     sequence to join; not null (may be empty)
     * @param separator placed between elements; not null (may be empty)
     */
    public static <T> String join(Iterable<T> items, String separator) {
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(separator, "separator");
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
            Iterable<T> items,
            String separator,
            Function<? super T, ? extends CharSequence> toString) {
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(separator, "separator");
        Objects.requireNonNull(toString, "toString");
        StringJoiner joiner = new StringJoiner(separator);
        for (T item : items) {
            joiner.add(item == null ? null : toString.apply(item));
        }
        return joiner.toString();
    }

    /**
     * Splits {@code input} on line breaks using {@code \n} or {@code \r\n} as separators
     * ({@link String#split(String)} with {@code \r?\n}).
     *
     * @param input the text; must not be {@code null}
     * @return an array of lines (may be a single element for text without newlines; behavior matches {@link String#split})
     * @throws NullPointerException if {@code input} is {@code null}
     */
    public static String[] splitLines(String input) {
        return input.split("\\r?\\n");
    }

    /**
     * Returns whether every character in {@code input} is a Unicode decimal digit ({@link Character#isDigit(char)}).
     * <p>The empty string is considered numeric ({@code true}).</p>
     *
     * @param input the string; must not be {@code null}
     * @return {@code true} if all characters are digits
     * @throws NullPointerException if {@code input} is {@code null}
     */
    public static boolean isNumeric(final String input) {
        return IntStream.range(0, input.length())
                .allMatch(i -> Character.isDigit(input.charAt(i)));
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
