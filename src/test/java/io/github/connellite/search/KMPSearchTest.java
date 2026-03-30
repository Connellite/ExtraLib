package io.github.connellite.search;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KMPSearchTest {

    @Test
    void performKMPSearch_singleMatch_string() {
        assertEquals(List.of(4), KMPSearch.performKMPSearch("abc def abc", "def"));
    }

    @Test
    void performKMPSearch_multipleMatches_string() {
        assertEquals(List.of(0, 4, 12), KMPSearch.performKMPSearch("abc abc def abc", "abc"));
    }

    @Test
    void performKMPSearch_overlappingMatches() {
        assertEquals(List.of(0, 1, 2), KMPSearch.performKMPSearch("aaaa", "aa"));
    }

    @Test
    void performKMPSearch_noMatch() {
        assertTrue(KMPSearch.performKMPSearch("hello world", "xyz").isEmpty());
    }

    @Test
    void performKMPSearch_emptyPattern() {
        assertTrue(KMPSearch.performKMPSearch("anything", "").isEmpty());
    }

    @Test
    void performKMPSearch_emptyText() {
        assertTrue(KMPSearch.performKMPSearch("", "a").isEmpty());
    }

    @Test
    void performKMPSearch_stringBuilder() {
        var text = new StringBuilder("prefix needle suffix needle");
        var pattern = new StringBuilder("needle");
        assertEquals(List.of(7, 21), KMPSearch.performKMPSearch(text, pattern));
    }

    @Test
    void performKMPSearch_stringBuffer() {
        var text = new StringBuffer("ababababa");
        var pattern = new StringBuffer("aba");
        assertEquals(List.of(0, 2, 4, 6), KMPSearch.performKMPSearch(text, pattern));
    }

    @Test
    void performKMPSearch_nullText() {
        assertThrows(NullPointerException.class, () -> KMPSearch.performKMPSearch(null, "a"));
    }

    @Test
    void performKMPSearch_nullPattern() {
        assertThrows(NullPointerException.class, () -> KMPSearch.performKMPSearch("a", null));
    }

    @Test
    void indexOfByteArray_found() {
        byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);
        byte[] pat = "world".getBytes(StandardCharsets.UTF_8);
        assertEquals(6, KMPSearch.indexOfByteArray(data, pat));
    }

    @Test
    void indexOfByteArray_notFound() {
        byte[] data = {1, 2, 3};
        byte[] pat = {9};
        assertEquals(-1, KMPSearch.indexOfByteArray(data, pat));
    }

    @Test
    void indexOfByteArray_emptyDataOrPattern() {
        assertEquals(-1, KMPSearch.indexOfByteArray(new byte[0], new byte[]{1}));
        assertEquals(-1, KMPSearch.indexOfByteArray(new byte[]{1}, new byte[0]));
    }

    @Test
    void indexOfByteArray_nullThrows() {
        assertThrows(NullPointerException.class, () -> KMPSearch.indexOfByteArray(null, new byte[1]));
        assertThrows(NullPointerException.class, () -> KMPSearch.indexOfByteArray(new byte[1], null));
    }
}
