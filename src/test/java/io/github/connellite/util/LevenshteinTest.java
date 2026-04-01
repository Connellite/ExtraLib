package io.github.connellite.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LevenshteinTest {

    @Test
    void dist_identical_isZero() {
        assertEquals(0, Levenshtein.dist("abc", "abc"));
    }

    @Test
    void dist_emptyFirst_isSecondLength() {
        assertEquals(5, Levenshtein.dist("", "hello"));
    }

    @Test
    void dist_emptySecond_isFirstLength() {
        assertEquals(3, Levenshtein.dist("abc", ""));
    }

    @Test
    void dist_bothEmpty_isZero() {
        assertEquals(0, Levenshtein.dist("", ""));
    }

    @Test
    void dist_kitten_sitting() {
        assertEquals(3, Levenshtein.dist("kitten", "sitting"));
    }

    @Test
    void dist_oneSubstitution() {
        assertEquals(1, Levenshtein.dist("cat", "cut"));
    }

    @Test
    void dist_stringBuilder() {
        assertEquals(
                1,
                Levenshtein.dist(new StringBuilder("ab"), new StringBuilder("abc")));
    }

    @Test
    void dist_nullThrows() {
        assertThrows(NullPointerException.class, () -> Levenshtein.dist(null, "a"));
        assertThrows(NullPointerException.class, () -> Levenshtein.dist("a", null));
    }
}
