package io.github.connellite.match;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KMPMatchingTest {

    @Test
    void isMatch_found_string() {
        assertTrue(KMPMatching.isMatch("abc def abc", "def"));
    }

    @Test
    void isMatch_notFound_string() {
        assertFalse(KMPMatching.isMatch("hello world", "xyz"));
    }

    @Test
    void isMatch_emptyPattern_isTrue() {
        assertTrue(KMPMatching.isMatch("anything", ""));
        assertTrue(KMPMatching.isMatch("", ""));
    }

    @Test
    void isMatch_singleChar() {
        assertTrue(KMPMatching.isMatch("z", "z"));
        assertFalse(KMPMatching.isMatch("z", "y"));
    }

    @Test
    void isMatch_repeatedPrefixPattern() {
        assertTrue(KMPMatching.isMatch("aaaa", "aaa"));
        assertFalse(KMPMatching.isMatch("aaa", "aaaa"));
    }

    @Test
    void performKMPSearch_singleCharPattern() {
        assertEquals(List.of(0, 1, 2), KMPMatching.performKMPSearch("aaa", "a"));
    }

    @Test
    void isMatch_stringBuilder() {
        var text = new StringBuilder("prefix needle suffix");
        var pattern = new StringBuilder("needle");
        assertTrue(KMPMatching.isMatch(text, pattern));
    }

    @Test
    void isMatch_stringBuffer_notFound() {
        var text = new StringBuffer("ababababa");
        var pattern = new StringBuffer("abba");
        assertFalse(KMPMatching.isMatch(text, pattern));
    }

    @Test
    void isMatch_nullThrows() {
        assertThrows(NullPointerException.class, () -> KMPMatching.isMatch(null, "a"));
        assertThrows(NullPointerException.class, () -> KMPMatching.isMatch("a", null));
    }

    @Test
    void performKMPSearch_singleMatch_string() {
        assertEquals(List.of(4), KMPMatching.performKMPSearch("abc def abc", "def"));
    }

    @Test
    void performKMPSearch_multipleMatches_string() {
        assertEquals(List.of(0, 4, 12), KMPMatching.performKMPSearch("abc abc def abc", "abc"));
    }

    @Test
    void performKMPSearch_overlappingMatches() {
        assertEquals(List.of(0, 1, 2), KMPMatching.performKMPSearch("aaaa", "aa"));
    }

    @Test
    void performKMPSearch_noMatch() {
        assertTrue(KMPMatching.performKMPSearch("hello world", "xyz").isEmpty());
    }

    @Test
    void performKMPSearch_emptyPattern() {
        assertTrue(KMPMatching.performKMPSearch("anything", "").isEmpty());
    }

    @Test
    void performKMPSearch_emptyText() {
        assertTrue(KMPMatching.performKMPSearch("", "a").isEmpty());
    }

    @Test
    void performKMPSearch_stringBuilder() {
        var text = new StringBuilder("prefix needle suffix needle");
        var pattern = new StringBuilder("needle");
        assertEquals(List.of(7, 21), KMPMatching.performKMPSearch(text, pattern));
    }

    @Test
    void performKMPSearch_stringBuffer() {
        var text = new StringBuffer("ababababa");
        var pattern = new StringBuffer("aba");
        assertEquals(List.of(0, 2, 4, 6), KMPMatching.performKMPSearch(text, pattern));
    }

    @Test
    void performKMPSearch_nullText() {
        assertThrows(NullPointerException.class, () -> KMPMatching.performKMPSearch(null, "a"));
    }

    @Test
    void performKMPSearch_nullPattern() {
        assertThrows(NullPointerException.class, () -> KMPMatching.performKMPSearch("a", null));
    }

    @Test
    void indexOfByteArray_found() {
        byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);
        byte[] pat = "world".getBytes(StandardCharsets.UTF_8);
        assertEquals(6, KMPMatching.indexOfByteArray(data, pat));
    }

    @Test
    void indexOfByteArray_notFound() {
        byte[] data = {1, 2, 3};
        byte[] pat = {9};
        assertEquals(-1, KMPMatching.indexOfByteArray(data, pat));
    }

    @Test
    void indexOfByteArray_emptyDataOrPattern() {
        assertEquals(-1, KMPMatching.indexOfByteArray(new byte[0], new byte[]{1}));
        assertEquals(-1, KMPMatching.indexOfByteArray(new byte[]{1}, new byte[0]));
    }

    @Test
    void indexOfByteArray_nullThrows() {
        assertThrows(NullPointerException.class, () -> KMPMatching.indexOfByteArray(null, new byte[1]));
        assertThrows(NullPointerException.class, () -> KMPMatching.indexOfByteArray(new byte[1], null));
    }
}
