package io.github.connellite.collections;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniqueArrayListTest {

    @Test
    void addIgnoresDuplicates() {
        UniqueArrayList<String> list = new UniqueArrayList<>();
        assertTrue(list.add("a"));
        assertFalse(list.add("a"));
        assertEquals(List.of("a"), list);
    }

    @Test
    void addAllPreservesOrderOfNewElements() {
        UniqueArrayList<String> list = new UniqueArrayList<>();
        list.add("x");
        assertTrue(list.addAll(List.of("x", "y", "x", "z")));
        assertEquals(List.of("x", "y", "z"), list);
    }

    @Test
    void setReplacesAndRemovesOtherOccurrence() {
        UniqueArrayList<String> list = new UniqueArrayList<>(List.of("a", "b", "c"));
        list.set(1, "c");
        assertEquals(List.of("a", "c"), list);
        assertTrue(list.contains("c"));
    }

    @Test
    void constructorFromCollectionDoesNotModifySourceList() {
        List<String> input = new ArrayList<>(List.of("a", "a", "b"));
        UniqueArrayList<String> u = new UniqueArrayList<>(input);
        assertEquals(List.of("a", "a", "b"), input);
        assertEquals(List.of("a", "b"), u);
    }

    @Test
    void listIteratorSetUnsupported() {
        UniqueArrayList<String> list = new UniqueArrayList<>(List.of("a"));
        var it = list.listIterator();
        it.next();
        assertThrows(UnsupportedOperationException.class, () -> it.set("b"));
    }

    @Test
    void subListIsUnmodifiable() {
        UniqueArrayList<String> list = new UniqueArrayList<>(List.of("a", "b"));
        List<String> sub = list.subList(0, 1);
        assertThrows(UnsupportedOperationException.class, () -> sub.add("x"));
    }
}
