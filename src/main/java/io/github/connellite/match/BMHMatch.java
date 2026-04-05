package io.github.connellite.match;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Boyer–Moore–style substring search over {@link CharSequence}.
 * <p>
 * {@link Strategy#BAD_CHARACTER} uses the bad-character rule only (classic BM shift on mismatch;
 * after a full match, shift uses the character aligned just past the window when possible).
 * {@link Strategy#GOOD_SUFFIX} adds strong good-suffix preprocessing (Boyer–Moore style) via
 * {@code shift}/{@code bpos} tables.
 * <p>
 * Both strategies compare UTF-16 code units ({@link CharSequence#charAt(int)}), like {@code String}.
 * Time is sublinear on average for long alphabets; worst case depends on the variant.
 */
@UtilityClass
public class BMHMatch {

    /**
     * Which Boyer–Moore-style variant {@link #performBMHSearch(CharSequence, CharSequence, Strategy)} uses.
     */
    public enum Strategy {
        /** Bad-character table only. */
        BAD_CHARACTER,
        /** Good-suffix preprocessing (strong suffix shifts). */
        GOOD_SUFFIX
    }

    private static final int CHAR_RANGE = Character.MAX_VALUE + 1;

    /**
     * @return {@code true} if {@code pattern} occurs in {@code text} at least once
     * @see #performBMHSearch(CharSequence, CharSequence, Strategy)
     */
    public static boolean isMatch(CharSequence text, CharSequence pattern) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.isEmpty()) {
            return true;
        }
        if (pattern.length() > text.length()) {
            return false;
        }
        return findFirstBadCharacter(text, pattern) >= 0;
    }

    /**
     * Finds all start indices of {@code pattern} in {@code text} using the chosen algorithm.
     *
     * @param text    text to search; not null
     * @param pattern  substring; not null (empty pattern yields an empty list)
     * @param strategy search variant; not null
     * @return mutable list of zero-based start indices in ascending order
     */
    public static List<Integer> performBMHSearch(CharSequence text, CharSequence pattern, Strategy strategy) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(pattern, "pattern");
        Objects.requireNonNull(strategy, "strategy");
        if (pattern.isEmpty()) {
            return new ArrayList<>();
        }
        if (pattern.length() > text.length()) {
            return new ArrayList<>();
        }
        return switch (strategy) {
            case BAD_CHARACTER -> searchBadCharacterAll(text, pattern);
            case GOOD_SUFFIX -> searchGoodSuffixAll(text, pattern);
        };
    }

    private static int findFirstBadCharacter(CharSequence text, CharSequence pattern) {
        int n = text.length();
        int m = pattern.length();
        int[] badchar = buildBadCharacterTable(pattern, m);
        int s = 0;
        while (s <= n - m) {
            int j = m - 1;
            while (j >= 0 && pattern.charAt(j) == text.charAt(s + j)) {
                j--;
            }
            if (j < 0) {
                return s;
            }
            int bc = badchar[text.charAt(s + j)];
            int v = j - bc;
            s += Math.max(1, v);
        }
        return -1;
    }

    private static List<Integer> searchBadCharacterAll(CharSequence text, CharSequence pattern) {
        int n = text.length();
        int m = pattern.length();
        List<Integer> found = new ArrayList<>();
        int[] badchar = buildBadCharacterTable(pattern, m);
        int s = 0;
        while (s <= n - m) {
            int j = m - 1;
            while (j >= 0 && pattern.charAt(j) == text.charAt(s + j)) {
                j--;
            }
            if (j < 0) {
                found.add(s);
                if (s + m < n) {
                    s += m - badchar[text.charAt(s + m)];
                } else {
                    s += 1;
                }
            } else {
                int bc = badchar[text.charAt(s + j)];
                int v = j - bc;
                s += Math.max(v, 1);
            }
        }
        return found;
    }

    private static int[] buildBadCharacterTable(CharSequence pattern, int m) {
        int[] badchar = new int[CHAR_RANGE];
        Arrays.fill(badchar, -1);
        for (int i = 0; i < m; i++) {
            badchar[pattern.charAt(i)] = i;
        }
        return badchar;
    }

    private static List<Integer> searchGoodSuffixAll(CharSequence text, CharSequence pattern) {
        int n = text.length();
        int m = pattern.length();
        char[] pat = patternToChars(pattern, m);
        int[] bpos = new int[m + 1];
        int[] shift = new int[m + 1];
        Arrays.fill(shift, 0);
        preprocessStrongSuffix(shift, bpos, pat, m);
        preprocessCase2(shift, bpos, m);

        List<Integer> found = new ArrayList<>();
        int s = 0;
        while (s <= n - m) {
            int j = m - 1;
            while (j >= 0 && pat[j] == text.charAt(s + j)) {
                j--;
            }
            if (j < 0) {
                found.add(s);
                s += shift[0];
            } else {
                s += shift[j + 1];
            }
        }
        return found;
    }

    private static char[] patternToChars(CharSequence pattern, int m) {
        char[] pat = new char[m];
        for (int i = 0; i < m; i++) {
            pat[i] = pattern.charAt(i);
        }
        return pat;
    }

    private static void preprocessStrongSuffix(int[] shift, int[] bpos, char[] pat, int m) {
        int i = m;
        int j = m + 1;
        bpos[i] = j;
        while (i > 0) {
            while (j <= m && pat[i - 1] != pat[j - 1]) {
                if (shift[j] == 0) {
                    shift[j] = j - i;
                }
                j = bpos[j];
            }
            i--;
            j--;
            bpos[i] = j;
        }
    }

    private static void preprocessCase2(int[] shift, int[] bpos, int m) {
        int j = bpos[0];
        for (int i = 0; i <= m; i++) {
            if (shift[i] == 0) {
                shift[i] = j;
            }
            if (i == j) {
                j = bpos[j];
            }
        }
    }
}
