package io.github.connellite.collections;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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
    void addAtIndexIgnoresDuplicate() {
        UniqueArrayList<String> list = new UniqueArrayList<>(List.of("a", "b"));
        list.add(1, "a");
        assertEquals(List.of("a", "b"), list);
    }

    @Test
    void addAllPreservesOrderOfNewElements() {
        UniqueArrayList<String> list = new UniqueArrayList<>();
        list.add("x");
        assertTrue(list.addAll(List.of("x", "y", "x", "z")));
        assertEquals(List.of("x", "y", "z"), list);
    }

    @Test
    void addAllAtIndexInsertsOnlyNewElements() {
        UniqueArrayList<String> list = new UniqueArrayList<>(List.of("m", "n"));
        assertTrue(list.addAll(1, List.of("m", "o", "n", "p")));
        assertEquals(List.of("m", "o", "p", "n"), list);
    }

    @Test
    void setReplacesAndRemovesOtherOccurrence() {
        UniqueArrayList<String> list = new UniqueArrayList<>(List.of("a", "b", "c"));
        list.set(1, "c");
        assertEquals(List.of("a", "c"), list);
        assertTrue(list.contains("c"));
    }

    @Test
    void setSameValueAtSameIndexKeepsList() {
        UniqueArrayList<String> list = new UniqueArrayList<>(List.of("a", "b"));
        assertEquals("b", list.set(1, "b"));
        assertEquals(List.of("a", "b"), list);
    }

    @Test
    void removeByObjectAndByIndex() {
        UniqueArrayList<String> list = new UniqueArrayList<>(List.of("a", "b", "c"));
        assertEquals("b", list.remove(1));
        assertFalse(list.contains("b"));
        assertTrue(list.remove("c"));
        assertEquals(List.of("a"), list);
    }

    @Test
    void removeAllAndClear() {
        UniqueArrayList<String> list = new UniqueArrayList<>(List.of("a", "b", "c"));
        assertTrue(list.removeAll(List.of("b", "x")));
        assertEquals(List.of("a", "c"), list);
        list.clear();
        assertTrue(list.isEmpty());
        assertFalse(list.contains("a"));
    }

    @Test
    void retainAllShrinksBackingList() {
        UniqueArrayList<String> list = new UniqueArrayList<>(List.of("a", "b", "c"));
        assertTrue(list.retainAll(List.of("b", "c")));
        assertEquals(List.of("b", "c"), list);
        assertFalse(list.retainAll(List.of("b", "c")));
    }

    @Test
    void retainAllUnchangedReturnsFalse() {
        UniqueArrayList<String> list = new UniqueArrayList<>(List.of("a", "b"));
        assertFalse(list.retainAll(List.of("a", "b")));
        assertEquals(List.of("a", "b"), list);
    }

    @Test
    void removeIf() {
        UniqueArrayList<String> list = new UniqueArrayList<>(List.of("a", "bb", "c"));
        assertTrue(list.removeIf(s -> s.length() > 1));
        assertEquals(List.of("a", "c"), list);
    }

    @Test
    void iteratorRemoveKeepsUniquesInSync() {
        UniqueArrayList<String> list = new UniqueArrayList<>(List.of("a", "b"));
        Iterator<String> it = list.iterator();
        it.next();
        it.remove();
        assertTrue(list.add("a"));
        assertEquals(List.of("b", "a"), list);
    }

    @Test
    void listIteratorAddSkipsDuplicate() {
        UniqueArrayList<String> list = new UniqueArrayList<>();
        list.add("a");
        ListIterator<String> it = list.listIterator();
        assertEquals("a", it.next());
        it.add("b");
        it.add("a");
        assertEquals(List.of("a", "b"), list);
    }

    @Test
    void containsAllUsesUniqueSemantics() {
        UniqueArrayList<String> list = new UniqueArrayList<>(List.of("a", "b"));
        assertTrue(list.containsAll(List.of("a", "b", "a")));
        assertFalse(list.containsAll(List.of("a", "c")));
    }

    @Test
    void cloneIndependentUniques() {
        UniqueArrayList<String> list = new UniqueArrayList<>(List.of("a", "b"));
        @SuppressWarnings("unchecked")
        UniqueArrayList<String> copy = (UniqueArrayList<String>) list.clone();
        assertNotSame(list, copy);
        copy.add("c");
        assertEquals(List.of("a", "b"), list);
        assertEquals(List.of("a", "b", "c"), copy);
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
