package io.github.connellite.match;

import lombok.experimental.UtilityClass;

import java.util.Objects;

/**
 * A modified version of the original Knuth-Morris-Pratt substring search algorithm which allows wildcards.
 *
 * @author Varun Shah
 */
@UtilityClass
public class WildKMPMatching {

    /**
     * Each {@code '*'} matches exactly one character; consecutive {@code '*'} characters must match the same
     * character in that run. A match is any window of {@code text} with length {@code pattern.length()} for which
     * {@link #matchesWindow(CharSequence, int, CharSequence)} is {@code true}.
     *
     * @param text    source text; not null
     * @param pattern wildcard pattern; not null
     * @return {@code true} when at least one match exists in {@code text}
     */
    public static boolean isMatch(CharSequence text, CharSequence pattern) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(pattern, "pattern");

        int textLength = text.length();
        int patternLength = pattern.length();
        if (patternLength == 0) {
            return true;
        }
        if (patternLength > textLength) {
            return false;
        }

        for (int start = 0; start <= textLength - patternLength; start++) {
            if (matchesWindow(text, start, pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether {@code pattern} matches the length-{@code pattern.length()} slice of {@code text} starting at {@code start}.
     * Each {@code '*'} matches one text character; consecutive {@code '*'} require the same character in that run.
     * <p>Package-private for tests; callers must ensure {@code start + pattern.length() <= text.length()}.
     */
    static boolean matchesWindow(CharSequence text, int start, CharSequence pattern) {
        int patternLength = pattern.length();
        Character groupWildcard = null;
        for (int j = 0; j < patternLength; j++) {
            char patternChar = pattern.charAt(j);
            char textChar = text.charAt(start + j);

            if (patternChar == '*') {
                if (groupWildcard == null) {
                    groupWildcard = textChar;
                } else if (groupWildcard != textChar) {
                    return false;
                }
            } else {
                groupWildcard = null;
                if (patternChar != textChar) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Given some text and a pattern, it searches for the first instance of the pattern in the text.<br>
     * An asterisk (*) in the pattern tells the algorithm to match on any character at that location in the text.
     *
     * @param text    The text to be searched
     * @param pattern The pattern to search for in the text
     * @return The starting index of the pattern in the text. If not found, -1 is returned.
     */
    public static int search(CharSequence text, CharSequence pattern) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(pattern, "pattern");

        final int textLength = text.length();
        final int patternLength = pattern.length();
        if (patternLength > textLength) {
            return -1;
        }

        final int[] prefixTable = getDFA(pattern);

        int matchLength = 0;
        Character wildLetter = null;
        for (int i = 0; i < textLength; i++) {
            // back-track on failure
            while (matchLength > 0 && pattern.charAt(matchLength) != text.charAt(i)) {
                // check if fail was due to wildcard
                if (pattern.charAt(matchLength) == '*') {
                    // if initial wildcard, set it
                    if (wildLetter == null) {
                        wildLetter = text.charAt(i);

                        // loop-back with KMP - double check already matched pattern
                        final int kmpValue = search(text.subSequence(i - matchLength, i), pattern.subSequence(0, matchLength));
                        if (kmpValue != 0) {
                            matchLength = 0; // reset match
                        } else if (pattern.charAt(matchLength - 1) == '*') {
                            wildLetter = text.charAt(i - 1); // reset wildcard
                        }
                        break;
                    } else if (wildLetter == text.charAt(i)) {
                        break; // wildcard matches
                    }
                }

                matchLength = prefixTable[matchLength - 1]; // fall-back
                wildLetter = null;

                // edge case - match previous seen for proper shift
                if (matchLength == 0 && pattern.charAt(matchLength + 1) == '*'
                        && text.charAt(i - 1) == pattern.charAt(matchLength)) {
                    matchLength++;
                }
            }

            // match or wildcard
            if (pattern.charAt(matchLength) == text.charAt(i) || pattern.charAt(matchLength) == '*') {
                // wildcard
                if (pattern.charAt(matchLength) == '*') {
                    if (wildLetter == null) {
                        wildLetter = text.charAt(i); // set wildcard
                    } else if (wildLetter != text.charAt(i)) {
                        // doesn't match current wildcard
                        if (matchLength == 1) {
                            wildLetter = text.charAt(i); // edge case, new wildcard
                            continue;
                        }
                        // reset
                        wildLetter = null;
                        matchLength = 0;
                        continue;
                    }
                } else {
                    wildLetter = null; // reset wildcard
                }
                matchLength++; // matched
            }

            // found the pattern
            if (matchLength == patternLength) {
                return i - (patternLength - 1);
            }
        }

        // couldn't find the pattern
        return -1;
    }

    /**
     * Prefix table used by {@link #search}; same shape as a KMP failure function over {@code pattern}.
     * Package-private for tests.
     *
     * @param pattern The pattern which is being searched in the text
     * @return table of length {@code pattern.length()} (empty when pattern is empty)
     */
    static int[] getDFA(CharSequence pattern) {

        final int length = pattern.length();
        if (length == 0) {
            return new int[0];
        }
        final int[] dfa = new int[length];
        dfa[0] = 0;
        int longestPrefixIndex = 0;

        for (int i = 2; i < length; i++) {
            // back-track
            while (longestPrefixIndex > 0 && pattern.charAt(longestPrefixIndex + 1) != pattern.charAt(i)) {
                longestPrefixIndex = dfa[longestPrefixIndex];
            }

            // match
            if (pattern.charAt(longestPrefixIndex + 1) == pattern.charAt(i)) {
                longestPrefixIndex++;
            }
            dfa[i] = longestPrefixIndex;
        }
        return dfa;
    }
}