package io.github.connellite.util.internal;

import lombok.experimental.UtilityClass;

/**
 * Index-range helpers for skipping leading/trailing space and control characters,
 * and optional wrapper brackets, without allocating a substring.
 */
@UtilityClass
public class CharBounds {

    public static int trimStart(CharSequence text, int start, int end) {
        while (start < end && isSpaceOrControl(text.charAt(start))) {
            start++;
        }
        return start;
    }

    public static int trimEnd(CharSequence text, int start, int end) {
        while (start < end && isSpaceOrControl(text.charAt(end - 1))) {
            end--;
        }
        return end;
    }

    public static boolean isSpaceOrControl(char ch) {
        return Character.isWhitespace(ch) || Character.isSpaceChar(ch) || Character.isISOControl(ch);
    }

    public static boolean isOpeningWrapper(char ch) {
        return ch == '[' || ch == '{';
    }

    public static boolean isClosingWrapper(char ch) {
        return ch == ']' || ch == '}';
    }

    public static int skipOpeningWrapper(CharSequence text, int start, int end) {
        if (start < end && isOpeningWrapper(text.charAt(start))) {
            return start + 1;
        }
        return start;
    }

    public static int skipClosingWrapper(CharSequence text, int start, int end) {
        if (start < end && isClosingWrapper(text.charAt(end - 1))) {
            return end - 1;
        }
        return end;
    }
}
