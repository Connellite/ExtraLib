package io.github.connellite.collections;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Thorough coverage for {@code NullSkipping*} collection/map implementations.
 */
class NullSkippingCollectionsTest {

    // --- NullSkippingArrayList ---

    @Test
    void arrayList_addAtIndexIgnoresNull() {
        NullSkippingArrayList<String> list = new NullSkippingArrayList<>(List.of("a"));
        list.add(0, null);
        assertEquals(List.of("a"), list);
        list.add(1, "b");
        assertEquals(List.of("a", "b"), list);
    }

    @Test
    void arrayList_addAllAtIndexSkipsNullsPreservesOrder() {
        NullSkippingArrayList<String> list = new NullSkippingArrayList<>(List.of("x"));
        assertTrue(list.addAll(1, Arrays.asList(null, "a", null, "b")));
        assertEquals(List.of("x", "a", "b"), list);
    }

    @Test
    void arrayList_addAllAtIndexEmptyCollectionReturnsFalse() {
        NullSkippingArrayList<String> list = new NullSkippingArrayList<>(List.of("x"));
        assertFalse(list.addAll(0, List.of()));
    }

    @Test
    void arrayList_constructorFromCollectionSkipsNulls() {
        NullSkippingArrayList<String> list = new NullSkippingArrayList<>(Arrays.asList("a", null, "b"));
        assertEquals(List.of("a", "b"), list);
    }

    @Test
    void arrayList_addAllOnlyNullsReturnsFalse() {
        NullSkippingArrayList<String> list = new NullSkippingArrayList<>();
        list.add("x");
        assertFalse(list.addAll(Arrays.asList(null, null)));
        assertEquals(List.of("x"), list);
    }

    // --- NullSkippingLinkedList ---

    @Test
    void linkedList_dequeSkipsNull() {
        NullSkippingLinkedList<String> d = new NullSkippingLinkedList<>();
        assertFalse(d.offer(null));
        assertFalse(d.offerFirst(null));
        assertFalse(d.offerLast(null));
        d.push(null);
        assertDoesNotThrow(() -> d.addFirst(null));
        assertDoesNotThrow(() -> d.addLast(null));
        assertTrue(d.isEmpty());
        d.addFirst("a");
        d.addLast("b");
        d.offerFirst("x");
        d.offerLast("y");
        d.push("z");
        assertEquals("z", d.getFirst());
    }

    @Test
    void linkedList_constructorAndAddAllAtIndex() {
        NullSkippingLinkedList<String> d = new NullSkippingLinkedList<>(Arrays.asList(null, "a", null));
        assertEquals(List.of("a"), d);
        assertTrue(d.addAll(1, Arrays.asList(null, "b")));
        assertEquals(List.of("a", "b"), d);
    }

    // --- NullSkippingArrayDeque ---

    @Test
    void arrayDeque_skipsNullOnAllInsertionPaths() {
        NullSkippingArrayDeque<String> d = new NullSkippingArrayDeque<>();
        assertFalse(d.add(null));
        assertDoesNotThrow(() -> d.addFirst(null));
        assertDoesNotThrow(() -> d.addLast(null));
        assertFalse(d.offerFirst(null));
        assertFalse(d.offerLast(null));
        d.push(null);
        assertTrue(d.isEmpty());
        d.addLast("a");
        d.addFirst("b");
        assertEquals("b", d.getFirst());
        assertEquals("a", d.getLast());
    }

    @Test
    void arrayDeque_constructorFromCollection() {
        NullSkippingArrayDeque<String> d = new NullSkippingArrayDeque<>(Arrays.asList(null, "x", null));
        assertEquals(1, d.size());
        assertTrue(d.contains("x"));
    }

    // --- NullSkippingLinkedHashSet ---

    @Test
    void linkedHashSet_skipsNullOnAddAndAddAll() {
        NullSkippingLinkedHashSet<String> set = new NullSkippingLinkedHashSet<>();
        assertFalse(set.add(null));
        set.add("a");
        assertTrue(set.addAll(Arrays.asList(null, "b", "a")));
        assertEquals(2, set.size());
        assertTrue(set.contains("b"));
    }

    // --- NullSkippingHashSet (addAll edge) ---

    @Test
    void hashSet_addAllNoChangeWhenOnlyNulls() {
        NullSkippingHashSet<String> set = new NullSkippingHashSet<>();
        set.add("x");
        assertFalse(set.addAll(Arrays.asList(null, null)));
        assertEquals(1, set.size());
    }

    // --- NullSkippingTreeSet ---

    @Test
    void treeSet_skipsNullPreservesOrder() {
        NullSkippingTreeSet<String> set = new NullSkippingTreeSet<>();
        assertFalse(set.add(null));
        set.addAll(Arrays.asList("b", null, "a"));
        assertEquals(List.of("a", "b"), new ArrayList<>(set));
    }

    @Test
    void treeSet_withComparator() {
        NullSkippingTreeSet<String> set = new NullSkippingTreeSet<>(Comparator.reverseOrder());
        set.add("a");
        set.add("b");
        assertEquals(List.of("b", "a"), new ArrayList<>(set));
    }

    // --- NullSkippingPriorityQueue ---

    @Test
    void priorityQueue_skipsNullOnAddOfferAndAddAll() {
        NullSkippingPriorityQueue<String> q = new NullSkippingPriorityQueue<>();
        assertFalse(q.add(null));
        assertFalse(q.offer(null));
        assertFalse(q.addAll(Arrays.asList(null, null)));
        assertTrue(q.offer("b"));
        assertTrue(q.add("a"));
        assertEquals(2, q.size());
    }

    @Test
    void priorityQueue_withComparatorConstructor() {
        NullSkippingPriorityQueue<String> q = new NullSkippingPriorityQueue<>(16, Comparator.reverseOrder());
        q.addAll(List.of("a", "b"));
        assertEquals("b", q.peek());
    }

    // --- NullSkippingHashMap ---

    @Test
    void hashMap_putAllSkipsNullKeys() {
        NullSkippingHashMap<String, Integer> m = new NullSkippingHashMap<>();
        Map<String, Integer> src = new LinkedHashMap<>();
        src.put("a", 1);
        src.put(null, 99);
        src.put("b", 2);
        m.putAll(src);
        assertEquals(2, m.size());
        assertFalse(m.containsKey(null));
        assertEquals(1, m.get("a"));
        assertEquals(2, m.get("b"));
    }

    @Test
    void hashMap_putIfAbsentAndMergeSkipNullKey() {
        NullSkippingHashMap<String, Integer> m = new NullSkippingHashMap<>();
        assertNull(m.putIfAbsent(null, 1));
        m.put("k", 1);
        assertNull(m.merge(null, 5, Integer::sum));
        assertEquals(1, m.get("k"));
        assertFalse(m.containsKey(null));
    }

    @Test
    void hashMap_mergeRequiresNonNullRemappingFunction() {
        NullSkippingHashMap<String, Integer> m = new NullSkippingHashMap<>();
        assertThrows(NullPointerException.class, () -> m.merge("a", 1, null));
    }

    // --- NullSkippingLinkedHashMap ---

    @Test
    void linkedHashMap_putAllSkipsNullKeysPreservesOrder() {
        NullSkippingLinkedHashMap<String, Integer> m = new NullSkippingLinkedHashMap<>();
        Map<String, Integer> src = new LinkedHashMap<>();
        src.put("b", 2);
        src.put(null, 0);
        src.put("a", 1);
        m.putAll(src);
        assertEquals(List.of("b", "a"), new ArrayList<>(m.keySet()));
    }

    @Test
    void linkedHashMap_mergeNullKeyNoOp() {
        NullSkippingLinkedHashMap<String, Integer> m = new NullSkippingLinkedHashMap<>();
        m.put("x", 1);
        assertNull(m.merge(null, 9, Integer::sum));
        assertEquals(1, m.size());
    }

    // --- NullSkippingTreeMap ---

    @Test
    void treeMap_constructorSkipsNullKeysFromSource() {
        TreeMap<String, Integer> src = new TreeMap<>();
        src.put("b", 2);
        src.put("a", 1);
        NullSkippingTreeMap<String, Integer> m = new NullSkippingTreeMap<>(src);
        assertEquals(2, m.size());
        Map<String, Integer> withNull = new LinkedHashMap<>();
        withNull.put("z", 3);
        withNull.put(null, 9);
        m.putAll(withNull);
        assertEquals(3, m.size());
        assertEquals(Integer.valueOf(3), m.get("z"));
    }

    @Test
    void treeMap_putIfAbsentNullKey_throwsLikeTreeMap() {
        NullSkippingTreeMap<String, Integer> m = new NullSkippingTreeMap<>();
        assertThrows(NullPointerException.class, () -> m.putIfAbsent(null, 1));
        assertTrue(m.isEmpty());
    }

    @Test
    void treeMap_copyConstructorFromSortedMap() {
        SortedMap<String, Integer> src = new TreeMap<>();
        src.put("b", 2);
        src.put("a", 1);
        NullSkippingTreeMap<String, Integer> m = new NullSkippingTreeMap<>(src);
        assertEquals("a", m.firstKey());
        assertEquals("b", m.lastKey());
    }
}
