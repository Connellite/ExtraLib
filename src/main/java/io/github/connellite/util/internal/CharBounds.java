package io.github.connellite.util.internal;

import lombok.experimental.UtilityClass;

/**
 * Index-range helpers for skipping leading/trailing whitespace and optional wrapper brackets
 * without allocating a substring.
 */
@UtilityClass
public class CharBounds {

    /**
     * @return the first index in {@code [start, end)} that is not whitespace, or {@code end} when
     * the whole range is whitespace
     */
    public static int trimStart(CharSequence text, int start, int end) {
        while (start < end && Character.isWhitespace(text.charAt(start))) {
            start++;
        }
        return start;
    }

    /**
     * @return one past the last index in {@code [start, end)} that is not whitespace, or
     * {@code start} when the whole range is whitespace
     */
    public static int trimEnd(CharSequence text, int start, int end) {
        while (start < end && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        return end;
    }

    /**
     * @return {@code true} for {@code '['} or {@code '{'}
     */
    public static boolean isOpeningWrapper(char ch) {
        return ch == '[' || ch == '{';
    }

    /**
     * @return {@code true} for {@code ']'} or {@code '}'}
     */
    public static boolean isClosingWrapper(char ch) {
        return ch == ']' || ch == '}';
    }

    /**
     * Advances {@code start} past a single opening wrapper when one is present.
     */
    public static int skipOpeningWrapper(CharSequence text, int start, int end) {
        if (start < end && isOpeningWrapper(text.charAt(start))) {
            return start + 1;
        }
        return start;
    }

    /**
     * Retreats {@code end} before a single closing wrapper when one is present.
     */
    public static int skipClosingWrapper(CharSequence text, int start, int end) {
        if (start < end && isClosingWrapper(text.charAt(end - 1))) {
            return end - 1;
        }
        return end;
    }
}
