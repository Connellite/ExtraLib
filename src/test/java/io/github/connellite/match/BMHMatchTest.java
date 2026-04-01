package io.github.connellite.match;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.connellite.match.BMHMatch.Strategy.BAD_CHARACTER;
import static io.github.connellite.match.BMHMatch.Strategy.GOOD_SUFFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BMHMatchTest {

    @Test
    void isMatch_found_string() {
        assertTrue(BMHMatch.isMatch("abc def abc", "def"));
    }

    @Test
    void isMatch_notFound_string() {
        assertFalse(BMHMatch.isMatch("hello world", "xyz"));
    }

    @Test
    void isMatch_emptyPattern_isTrue() {
        assertTrue(BMHMatch.isMatch("anything", ""));
        assertTrue(BMHMatch.isMatch("", ""));
    }

    @Test
    void isMatch_stringBuilder() {
        var text = new StringBuilder("prefix needle suffix");
        var pattern = new StringBuilder("needle");
        assertTrue(BMHMatch.isMatch(text, pattern));
    }

    @Test
    void isMatch_stringBuffer_notFound() {
        var text = new StringBuffer("ababababa");
        var pattern = new StringBuffer("abba");
        assertFalse(BMHMatch.isMatch(text, pattern));
    }

    @Test
    void isMatch_nullThrows() {
        assertThrows(NullPointerException.class, () -> BMHMatch.isMatch(null, "a"));
        assertThrows(NullPointerException.class, () -> BMHMatch.isMatch("a", null));
    }

    @Test
    void performBMHSearch_badCharacter_matchesKmp_single() {
        assertEquals(
                KMPMatching.performKMPSearch("abc def abc", "def"),
                BMHMatch.performBMHSearch("abc def abc", "def", BAD_CHARACTER));
    }

    @Test
    void performBMHSearch_goodSuffix_matchesKmp_single() {
        assertEquals(
                KMPMatching.performKMPSearch("abc def abc", "def"),
                BMHMatch.performBMHSearch("abc def abc", "def", GOOD_SUFFIX));
    }

    @Test
    void performBMHSearch_bothMethods_agree_multiple() {
        String text = "abc abc def abc";
        String pat = "abc";
        List<Integer> bad = BMHMatch.performBMHSearch(text, pat, BAD_CHARACTER);
        List<Integer> good = BMHMatch.performBMHSearch(text, pat, GOOD_SUFFIX);
        assertEquals(List.of(0, 4, 12), bad);
        assertEquals(bad, good);
        assertEquals(KMPMatching.performKMPSearch(text, pat), bad);
    }

    @Test
    void performBMHSearch_overlappingMatches() {
        assertEquals(
                KMPMatching.performKMPSearch("aaaa", "aa"),
                BMHMatch.performBMHSearch("aaaa", "aa", BAD_CHARACTER));
        assertEquals(
                KMPMatching.performKMPSearch("aaaa", "aa"),
                BMHMatch.performBMHSearch("aaaa", "aa", GOOD_SUFFIX));
    }

    @Test
    void performBMHSearch_noMatch() {
        assertTrue(BMHMatch.performBMHSearch("hello world", "xyz", BAD_CHARACTER).isEmpty());
        assertTrue(BMHMatch.performBMHSearch("hello world", "xyz", GOOD_SUFFIX).isEmpty());
    }

    @Test
    void performBMHSearch_emptyPattern() {
        assertTrue(BMHMatch.performBMHSearch("anything", "", BAD_CHARACTER).isEmpty());
        assertTrue(BMHMatch.performBMHSearch("anything", "", GOOD_SUFFIX).isEmpty());
    }

    @Test
    void performBMHSearch_emptyText() {
        assertTrue(BMHMatch.performBMHSearch("", "a", BAD_CHARACTER).isEmpty());
        assertTrue(BMHMatch.performBMHSearch("", "a", GOOD_SUFFIX).isEmpty());
    }

    @Test
    void performBMHSearch_stringBuilder() {
        var text = new StringBuilder("prefix needle suffix needle");
        var pattern = new StringBuilder("needle");
        assertEquals(
                List.of(7, 21),
                BMHMatch.performBMHSearch(text, pattern, BAD_CHARACTER));
        assertEquals(
                List.of(7, 21),
                BMHMatch.performBMHSearch(text, pattern, GOOD_SUFFIX));
    }

    @Test
    void performBMHSearch_stringBuffer() {
        var text = new StringBuffer("ababababa");
        var pattern = new StringBuffer("aba");
        assertEquals(
                KMPMatching.performKMPSearch(text, pattern),
                BMHMatch.performBMHSearch(text, pattern, BAD_CHARACTER));
    }

    @Test
    void performBMHSearch_nullStrategy() {
        assertThrows(
                NullPointerException.class,
                () -> BMHMatch.performBMHSearch("a", "a", null));
    }

    @Test
    void performBMHSearch_nullText() {
        assertThrows(NullPointerException.class, () -> BMHMatch.performBMHSearch(null, "a", BAD_CHARACTER));
    }

    @Test
    void performBMHSearch_nullPattern() {
        assertThrows(NullPointerException.class, () -> BMHMatch.performBMHSearch("a", null, BAD_CHARACTER));
    }

    @Test
    void performBMHSearch_repeatedCharPattern_matchesKmp() {
        String text = "aaaaaaaa";
        String pat = "aaa";
        assertEquals(KMPMatching.performKMPSearch(text, pat), BMHMatch.performBMHSearch(text, pat, BAD_CHARACTER));
        assertEquals(KMPMatching.performKMPSearch(text, pat), BMHMatch.performBMHSearch(text, pat, GOOD_SUFFIX));
    }
}
