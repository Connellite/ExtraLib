package io.github.connellite.search;

import lombok.experimental.UtilityClass;

import java.util.Objects;

/**
 * Glob-style wildcard matching for character sequences.
 * <p>
 * {@code '?'} matches exactly one arbitrary character; {@code '*'} matches any sequence of characters
 * (including empty). Matching is greedy in the dynamic-programming sense: the full text matches the
 * full pattern if some assignment of {@code *} spans satisfies all literals and {@code '?'}.
 * <p>
 * Time and space complexity are O(m·n) for text length m and pattern length n.
 */
@UtilityClass
public class WildcardMatching {

    /**
     * Returns whether {@code text} matches {@code pattern}.
     *
     * @param <T>     concrete character sequence type shared by both arguments
     * @param text    text to match; not null
     * @param pattern pattern with {@code '?'} and {@code '*'} wildcards; not null
     * @return {@code true} if the whole {@code text} matches the whole {@code pattern}
     */
    public static <T extends CharSequence> boolean isMatch(T text, T pattern) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(pattern, "pattern");

        int m = text.length();
        int n = pattern.length();

        boolean[][] dp = new boolean[m + 1][n + 1];
        dp[0][0] = true;

        for (int j = 1; j <= n; j++) {
            if (pattern.charAt(j - 1) == '*') {
                dp[0][j] = dp[0][j - 1];
            }
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                char textChar = text.charAt(i - 1);
                char patternChar = pattern.charAt(j - 1);

                if (patternChar == textChar || patternChar == '?') {
                    dp[i][j] = dp[i - 1][j - 1];
                } else if (patternChar == '*') {
                    dp[i][j] = dp[i - 1][j] || dp[i][j - 1];
                } else {
                    dp[i][j] = false;
                }
            }
        }

        return dp[m][n];
    }
}
