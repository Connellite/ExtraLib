package io.github.connellite.match;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for WildKMP.
 */
public class WildKMPMatchingTest {

    @Test
    public void isMatchBasic() {
        assertTrue(WildKMPMatching.isMatch("ACGT", "ACGT"));
        assertTrue(WildKMPMatching.isMatch("ACGTACAT", "ACAT"));
        assertFalse(WildKMPMatching.isMatch("ACGT", "TGCA"));
    }

    @Test
    public void isMatchWildcardRuns() {
        assertTrue(WildKMPMatching.isMatch("HUUGE", "H**GE"));
        assertTrue(WildKMPMatching.isMatch("HUUUGEHUUGEE", "H**G**"));
        assertFalse(WildKMPMatching.isMatch("HINGE", "H**GE"));
    }

    @Test
    public void isMatchCharSequenceTypes() {
        StringBuilder text = new StringBuilder("AAACCBAAB");
        StringBuilder pattern = new StringBuilder("A**B");
        assertTrue(WildKMPMatching.isMatch(text, pattern));

        StringBuffer text2 = new StringBuffer("ACCCABABB");
        StringBuffer pattern2 = new StringBuffer("A**B");
        assertFalse(WildKMPMatching.isMatch(text2, pattern2));
    }

    @Test
    public void isMatchEmptyAndNull() {
        assertTrue(WildKMPMatching.isMatch("", ""));
        assertFalse(WildKMPMatching.isMatch("", "A"));
        assertThrows(NullPointerException.class, () -> WildKMPMatching.isMatch(null, "A"));
        assertThrows(NullPointerException.class, () -> WildKMPMatching.isMatch("A", null));
    }

    @Test
    public void search_nullArgumentsThrow() {
        assertThrows(NullPointerException.class, () -> WildKMPMatching.search(null, "a"));
        assertThrows(NullPointerException.class, () -> WildKMPMatching.search("a", null));
    }

    @Test
    public void search_emptyPattern_throwsStringIndexOutOfBounds() {
        assertThrows(StringIndexOutOfBoundsException.class, () -> WildKMPMatching.search("x", ""));
    }

    @Test
    public void search_patternLongerThanText_returnsMinusOne() {
        assertEquals(-1, WildKMPMatching.search("ab", "abc"));
        assertEquals(-1, WildKMPMatching.search("", "a"));
    }

    @ParameterizedTest
    @CsvSource({
            "banana,ana,1",
            "aaaa,aa,0",
            "abcdef,def,3",
    })
    public void search_literalPatterns_matchKmpFirstIndex(String text, String pattern, int expected) {
        assertEquals(expected, WildKMPMatching.search(text, pattern));
        assertEquals(KMPMatching.performKMPSearch(text, pattern).get(0), WildKMPMatching.search(text, pattern));
    }

    @Test
    public void search_returnsFirstOccurrence_whenMultipleWindowsMatch() {
        assertEquals(0, WildKMPMatching.search("abab", "a*a"));
        assertTrue(WildKMPMatching.isMatch("abab", "a*a"));
    }

    @Test
    public void isMatch_falseWhenNoWindowFits_consecutiveStarsDifferentChars() {
        assertFalse(WildKMPMatching.isMatch("ab", "a*b"));
        assertEquals(-1, WildKMPMatching.search("ab", "a*b"));
    }

    @Test
    public void matchesWindow_examples() {
        assertTrue(WildKMPMatching.matchesWindow("HUUGE", 0, "H**GE"));
        assertFalse(WildKMPMatching.matchesWindow("HINGE", 0, "H**GE"));
        assertTrue(WildKMPMatching.matchesWindow("AAACCBAAB", 2, "A**B"));
        assertFalse(WildKMPMatching.matchesWindow("ACCCABABB", 2, "A**B"));
    }

    @Test
    public void getDFA_snapshotSmallPatterns() {
        assertArrayEquals(new int[0], WildKMPMatching.getDFA(""));
        assertArrayEquals(new int[]{0}, WildKMPMatching.getDFA("a"));
        assertArrayEquals(new int[]{0, 0, 1, 2}, WildKMPMatching.getDFA("aaaa"));
        assertArrayEquals(new int[]{0, 0, 0, 1}, WildKMPMatching.getDFA("abab"));
    }

    @Test
    public void isMatch_equalsDisjunctionOfMatchesWindow_exhaustiveSmallStrings() {
        char[] alpha = {'a', 'b', '*'};
        for (int tl = 0; tl <= 4; tl++) {
            for (int pl = 0; pl <= Math.min(3, tl + 1); pl++) {
                int finalPl = pl;
                forEachString(alpha, tl, text -> forEachString(alpha, finalPl, pattern ->
                        assertEquals(
                                existsAnyWindow(text, pattern),
                                WildKMPMatching.isMatch(text, pattern),
                                () -> "text=" + text + " pattern=" + pattern)));
            }
        }
    }

    @Test
    public void isMatch_equalsDisjunctionOfMatchesWindow_pseudorandomLongerStrings() {
        Random rnd = new Random(0xC0FFEE);
        String alphabet = "abc*";
        for (int trial = 0; trial < 8000; trial++) {
            int tl = 1 + rnd.nextInt(14);
            int pl = 1 + rnd.nextInt(Math.min(10, tl));
            StringBuilder text = new StringBuilder(tl);
            for (int i = 0; i < tl; i++) {
                text.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
            }
            StringBuilder pattern = new StringBuilder(pl);
            for (int i = 0; i < pl; i++) {
                pattern.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
            }
            assertEquals(
                    existsAnyWindow(text, pattern),
                    WildKMPMatching.isMatch(text, pattern),
                    () -> "text=" + text + " pattern=" + pattern);
        }
    }

    private static boolean existsAnyWindow(CharSequence text, CharSequence pattern) {
        int textLength = text.length();
        int patternLength = pattern.length();
        if (patternLength == 0) {
            return true;
        }
        if (patternLength > textLength) {
            return false;
        }
        for (int start = 0; start <= textLength - patternLength; start++) {
            if (WildKMPMatching.matchesWindow(text, start, pattern)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testAll() {
        assertEquals(0, WildKMPMatching.search("ACGT", "ACGT"));
        assertEquals(4, WildKMPMatching.search("ACGTACAT", "ACAT"));
        assertEquals(0, WildKMPMatching.search("ACGTACAT", "ACGT"));
        assertEquals(2, WildKMPMatching.search("AAAAAT", "AAAT"));
        assertEquals(8, WildKMPMatching.search("ACAT ACGACACAGT", "ACACAGT"));
        assertEquals(0, WildKMPMatching.search("ACGTACAT", "AC*T"));
        assertEquals(4, WildKMPMatching.search("AUGEHIGE", "H*GE"));
        assertEquals(0, WildKMPMatching.search("AUGEHIGE", "*UGE"));
        assertEquals(4, WildKMPMatching.search("AUGEHIGE", "HIG*"));
        assertEquals(0, WildKMPMatching.search("HUGEHUGS", "HUG*"));
        assertEquals(-1, WildKMPMatching.search("HUGEHUGS", "HIG*"));
        assertEquals(6, WildKMPMatching.search("HUGEAFHUGEEE", "HU*E*E"));
        assertEquals(5, WildKMPMatching.search("HOOLIHUUGEE", "H**GE"));
        assertEquals(0, WildKMPMatching.search("HUUGE", "H**GE"));
        assertEquals(-1, WildKMPMatching.search("HINGE", "H**GE"));
        assertEquals(-1, WildKMPMatching.search("ACCCABABB", "A**B"));
        assertEquals(10, WildKMPMatching.search("HIIIHINGGEHUUUGE", "H***GE"));
        assertEquals(2, WildKMPMatching.search("AAACCBAAB", "A**B"));
        assertEquals(-1, WildKMPMatching.search("AAACCBAAB", "***B"));
        assertEquals(-1, WildKMPMatching.search("AAACCBAAB", "B***"));
        assertEquals(6, WildKMPMatching.search("AAAACCABBBB", "A***B"));
        assertEquals(0, WildKMPMatching.search("HUUGEE", "H**G**"));
        assertEquals(6, WildKMPMatching.search("HUUUGEHUUGEE", "H**G**"));
        assertEquals(1, WildKMPMatching.search("HUUUGEHUUGEE", "***"));
        assertEquals(5, WildKMPMatching.search("AAABBAAABBB", "AAA***"));
        assertEquals(-1, WildKMPMatching.search("HUUGGGCCB", "H**G**B"));
        assertEquals(3, WildKMPMatching.search("AAACCCBAAB", "***B**"));
        assertEquals(8, WildKMPMatching.search("AAACCCBABBBBCC", "***B**"));
        assertEquals(3, WildKMPMatching.search("AACAAT", "A*T"));
        assertEquals(3, WildKMPMatching.search("AAAAAT", "A*T"));
        assertEquals(9, WildKMPMatching.search("AAAAAACCDAAAAAAD", "A**A**D"));
        assertEquals(-1, WildKMPMatching.search("babcacabca", "aa*c"));
    }

    private static void forEachString(char[] alpha, int len, Consumer<StringBuilder> consumer) {
        int[] idx = new int[len];
        Arrays.fill(idx, 0);
        while (true) {
            StringBuilder sb = new StringBuilder(len);
            for (int i = 0; i < len; i++) {
                sb.append(alpha[idx[i]]);
            }
            consumer.accept(sb);
            int p = len - 1;
            while (p >= 0 && idx[p] == alpha.length - 1) {
                idx[p] = 0;
                p--;
            }
            if (p < 0) {
                break;
            }
            idx[p]++;
        }
    }
}
