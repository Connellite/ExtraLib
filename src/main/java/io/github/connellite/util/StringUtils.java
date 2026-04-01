package io.github.connellite.util;

import lombok.experimental.UtilityClass;

import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Function;

/**
 * String helpers for joining {@link Iterable} elements with a separator.
 * Null elements are handled like {@link String#join(CharSequence, Iterable)} (via {@link StringJoiner}).
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
}
