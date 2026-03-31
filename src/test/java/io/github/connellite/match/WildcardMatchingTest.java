package io.github.connellite.match;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WildcardMatchingTest {

    @Test
    void emptyMatchesEmpty() {
        assertTrue(WildcardMatching.isMatch("", ""));
    }

    @Test
    void starMatchesEmpty() {
        assertTrue(WildcardMatching.isMatch("", "*"));
        assertTrue(WildcardMatching.isMatch("", "**"));
    }

    @Test
    void exactLiteral() {
        assertTrue(WildcardMatching.isMatch("abc", "abc"));
        assertFalse(WildcardMatching.isMatch("abc", "abd"));
    }

    @Test
    void questionMarkSingleChar() {
        assertTrue(WildcardMatching.isMatch("a", "?"));
        assertTrue(WildcardMatching.isMatch("ab", "a?"));
        assertFalse(WildcardMatching.isMatch("ab", "?"));
    }

    @Test
    void starAnySuffix() {
        assertTrue(WildcardMatching.isMatch("anything", "*"));
        assertTrue(WildcardMatching.isMatch("hello", "h*o"));
        assertTrue(WildcardMatching.isMatch("hello", "hel*o"));
    }

    @Test
    void starZeroChars() {
        assertTrue(WildcardMatching.isMatch("x", "*x"));
        assertTrue(WildcardMatching.isMatch("x", "x*"));
    }

    @Test
    void mixedWildcards() {
        assertTrue(WildcardMatching.isMatch("adceb", "*a*b"));
        assertFalse(WildcardMatching.isMatch("aab", "c*a*b"));
        assertFalse(WildcardMatching.isMatch("mississippi", "m??*ss*?i*pi"));
    }

    @Test
    void noMatchNonEmptyPattern() {
        assertFalse(WildcardMatching.isMatch("a", ""));
        assertFalse(WildcardMatching.isMatch("", "a"));
    }

    @Test
    void stringBuilder() {
        var text = new StringBuilder("fooBar");
        var pattern = new StringBuilder("foo?ar");
        assertTrue(WildcardMatching.isMatch(text, pattern));
    }

    @Test
    void stringBuffer() {
        var text = new StringBuffer("test");
        var pattern = new StringBuffer("t*st");
        assertTrue(WildcardMatching.isMatch(text, pattern));
    }

    @Test
    void nullText() {
        assertThrows(NullPointerException.class, () -> WildcardMatching.isMatch(null, "*"));
    }

    @Test
    void nullPattern() {
        assertThrows(NullPointerException.class, () -> WildcardMatching.isMatch("a", null));
    }
}
