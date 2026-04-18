package io.github.connellite.collections;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkedCaseInsensitiveSetTest {

    @Test
    void add_contains_remove_ignoreCase() {
        Set<String> s = new LinkedCaseInsensitiveSet();
        assertTrue(s.add("Foo"));
        assertFalse(s.add("foo"));
        assertTrue(s.contains("FOO"));
        assertEquals(1, s.size());
        assertTrue(s.remove("fOo"));
        assertTrue(s.isEmpty());
    }

    @Test
    void keepsInsertionOrderForDistinctLogicalKeys() {
        Set<String> s = new LinkedCaseInsensitiveSet();
        s.add("B");
        s.add("a");
        assertEquals(List.of("B", "a"), List.copyOf(s));
    }

    @Test
    void iteratorRemove_syncsBackingMap() {
        Set<String> s = new LinkedCaseInsensitiveSet();
        s.add("K");
        Iterator<String> it = s.iterator();
        it.next();
        it.remove();
        assertTrue(s.isEmpty());
        assertFalse(s.contains("k"));
    }

    @Test
    void addAll_collapsesLogicalDuplicates() {
        Set<String> s = new LinkedCaseInsensitiveSet();
        s.addAll(List.of("a", "A", "b"));
        assertEquals(2, s.size());
        assertEquals(List.of("A", "b"), List.copyOf(s));
    }

    @Test
    void nullElement_likeLinkedHashSet() {
        Set<String> s = new LinkedCaseInsensitiveSet();
        assertTrue(s.add(null));
        assertFalse(s.add(null));
        assertTrue(s.contains(null));
        assertTrue(s.remove(null));
    }

    @Test
    void cloneIndependent() {
        LinkedCaseInsensitiveSet s = new LinkedCaseInsensitiveSet();
        s.add("A");
        LinkedCaseInsensitiveSet c = s.clone();
        assertNotSame(s, c);
        c.add("a");
        assertEquals(1, s.size());
        assertEquals(1, c.size());
        c.add("B");
        assertEquals(2, c.size());
        assertEquals(1, s.size());
    }

    @Test
    void constructorWithCollectionAndLocale() {
        LinkedCaseInsensitiveSet s = new LinkedCaseInsensitiveSet(List.of("X"), Locale.ROOT);
        assertEquals(Locale.ROOT, s.getLocale());
        assertTrue(s.contains("x"));
    }

    @Test
    void lastAddWinsSpellingPreservesEarlierInsertionPositionsOfOtherKeys() {
        LinkedCaseInsensitiveSet s = new LinkedCaseInsensitiveSet();
        s.add("Aa");
        s.add("B");
        s.add("AA");
        assertEquals(List.of("B", "AA"), List.copyOf(s));
    }

    @Test
    void containsNonStringReturnsFalse() {
        LinkedCaseInsensitiveSet s = new LinkedCaseInsensitiveSet();
        s.add("a");
        assertFalse(s.contains(1L));
    }
}
