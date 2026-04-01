package io.github.connellite.match;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Knuth–Morris–Pratt (KMP) substring search for {@link CharSequence} and byte arrays.
 * <p>
 * Time complexity is O(n + m) for text length n and pattern length m.
 */
@UtilityClass
public class KMPMatching {

    /**
     * Independent KMP implementation.
     *
     * @param text    source text; not null
     * @param pattern searched substring; not null
     * @return {@code true} if {@code text} contains {@code pattern}
     */
    public static boolean isMatch(CharSequence text, CharSequence pattern) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.isEmpty()) {
            return true;
        }
        if (text.isEmpty() || pattern.length() > text.length()) {
            return false;
        }

        int[] lps = compileLps(pattern);

        for (int i = 0, j = 0; i < text.length(); ) {
            if (text.charAt(i) == pattern.charAt(j)) {
                i++;
                j++;
                if (j == pattern.length()) {
                    return true;
                }
            } else if (j > 0) {
                j = lps[j - 1];
            } else {
                i++;
            }
        }

        return false;
    }

    /**
     * Finds all starting indices of {@code pattern} in {@code text}.
     *
     * @param text    text to search; not null
     * @param pattern substring to find; not null (empty pattern yields an empty result)
     * @return mutable list of zero-based start indices, in ascending order
     */
    public static List<Integer> performKMPSearch(CharSequence text, CharSequence pattern) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.isEmpty()) {
            return new ArrayList<>();
        }
        int[] lps = compileLps(pattern);
        int textIndex = 0;
        int patternIndex = 0;
        List<Integer> foundIndexes = new ArrayList<>();

        int textLen = text.length();
        int patternLen = pattern.length();

        while (textIndex < textLen) {
            if (pattern.charAt(patternIndex) == text.charAt(textIndex)) {
                patternIndex++;
                textIndex++;
            }

            if (patternIndex == patternLen) {
                foundIndexes.add(textIndex - patternIndex);
                patternIndex = lps[patternIndex - 1];
            } else if (textIndex < textLen && pattern.charAt(patternIndex) != text.charAt(textIndex)) {
                if (patternIndex != 0) {
                    patternIndex = lps[patternIndex - 1];
                } else {
                    textIndex++;
                }
            }
        }

        return foundIndexes;
    }

    /**
     * Builds the longest proper prefix which is also a suffix (LPS) table for KMP.
     *
     * @param pattern non-null, non-empty pattern
     * @return LPS array of length {@code pattern.length()}
     */
    private static int[] compileLps(CharSequence pattern) {
        int patternLength = pattern.length();
        int len = 0;
        int i = 1;
        int[] lps = new int[patternLength];
        lps[0] = 0;

        while (i < patternLength) {
            if (pattern.charAt(i) == pattern.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
            } else if (len != 0) {
                len = lps[len - 1];
            } else {
                lps[i] = len;
                i++;
            }
        }

        return lps;
    }

    /**
     * Returns the zero-based index of the first occurrence of {@code pattern} in {@code data},
     * or {@code -1} if not found.
     *
     * @param data    bytes to search; not null
     * @param pattern byte sequence to find; not null
     * @return start index of first match, or {@code -1} if {@code data} or {@code pattern} is empty
     *         or no match exists
     */
    public static int indexOfByteArray(byte[] data, byte[] pattern) {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(pattern, "pattern");
        if (data.length == 0 || pattern.length == 0) {
            return -1;
        }
        int[] failure = computeFailure(pattern);
        int j = 0;

        for (int i = 0; i < data.length; i++) {
            while (j > 0 && pattern[j] != data[i]) {
                j = failure[j - 1];
            }

            if (pattern[j] == data[i]) {
                j++;
            }

            if (j == pattern.length) {
                return i - pattern.length + 1;
            }
        }

        return -1;
    }

    /**
     * LPS / failure function for byte-array KMP ({@link #indexOfByteArray}).
     *
     * @param pattern non-null pattern
     * @return failure array of length {@code pattern.length}
     */
    private static int[] computeFailure(byte[] pattern) {
        int[] failure = new int[pattern.length];
        int j = 0;

        for (int i = 1; i < pattern.length; i++) {
            while (j > 0 && pattern[j] != pattern[i]) {
                j = failure[j - 1];
            }

            if (pattern[j] == pattern[i]) {
                j++;
            }

            failure[i] = j;
        }

        return failure;
    }
}
