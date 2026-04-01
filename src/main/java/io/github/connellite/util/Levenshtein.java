package io.github.connellite.util;

import lombok.experimental.UtilityClass;

import java.util.Objects;

/**
 * Levenshtein (edit) distance: minimum single-character insertions, deletions, and substitutions
 * needed to turn one {@link CharSequence} into another. Uses UTF-16 code units, like {@link String}.
 * <p>
 * Time O(m·n), space O(n) for lengths m and n (two rows).
 */
@UtilityClass
public class Levenshtein {

    /**
     * @param word1 first sequence; not null
     * @param word2 second sequence; not null
     * @return edit distance between {@code word1} and {@code word2} (non-negative)
     */
    public static int dist(CharSequence word1, CharSequence word2) {
        Objects.requireNonNull(word1, "word1");
        Objects.requireNonNull(word2, "word2");
        int m = word1.length();
        int n = word2.length();
        int[] prev = new int[n + 1];
        for (int j = 0; j <= n; j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= m; i++) {
            int[] curr = new int[n + 1];
            curr[0] = i;
            char c1 = word1.charAt(i - 1);
            for (int j = 1; j <= n; j++) {
                int d1 = prev[j] + 1;
                int d2 = curr[j - 1] + 1;
                int d3 = prev[j - 1];
                if (c1 != word2.charAt(j - 1)) {
                    d3++;
                }
                curr[j] = Math.min(Math.min(d1, d2), d3);
            }
            prev = curr;
        }
        return prev[n];
    }
}
