package io.github.connellite.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

class CaseInsensitiveHashSetTest {

    @Test
    void add_contains_remove_ignoreCase() {
        Set<String> s = new CaseInsensitiveHashSet();
        assertTrue(s.add("Foo"));
        assertFalse(s.add("foo"));
        assertTrue(s.contains("FOO"));
        assertEquals(1, s.size());
        assertTrue(s.remove("fOo"));
        assertTrue(s.isEmpty());
    }

    @Test
    void lastAddWinsSpellingInIteration() {
        Set<String> s = new CaseInsensitiveHashSet();
        s.add("Aa");
        s.add("AA");
        Iterator<String> it = s.iterator();
        assertTrue(it.hasNext());
        assertEquals("AA", it.next());
    }

    @Test
    void iteratorRemove_syncsBackingMap() {
        Set<String> s = new CaseInsensitiveHashSet();
        s.add("K");
        Iterator<String> it = s.iterator();
        it.next();
        it.remove();
        assertTrue(s.isEmpty());
        assertFalse(s.contains("k"));
    }

    @Test
    void addAll_collapsesLogicalDuplicates() {
        Set<String> s = new CaseInsensitiveHashSet();
        s.addAll(List.of("a", "A", "b"));
        assertEquals(2, s.size());
    }

    @Test
    void nullElement_likeHashSet() {
        Set<String> s = new CaseInsensitiveHashSet();
        assertTrue(s.add(null));
        assertFalse(s.add(null));
        assertTrue(s.contains(null));
        assertTrue(s.remove(null));
    }

    @Test
    void cloneIndependent() {
        CaseInsensitiveHashSet s = new CaseInsensitiveHashSet();
        s.add("A");
        CaseInsensitiveHashSet c = s.clone();
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
        CaseInsensitiveHashSet s = new CaseInsensitiveHashSet(List.of("X"), Locale.ROOT);
        assertEquals(Locale.ROOT, s.getLocale());
        assertTrue(s.contains("x"));
    }
}
