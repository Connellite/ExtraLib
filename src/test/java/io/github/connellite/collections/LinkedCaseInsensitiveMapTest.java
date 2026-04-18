package io.github.connellite.collections;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkedCaseInsensitiveMapTest {

    @Test
    void put_preservesInsertionOrderAndCaseInsensitiveLookup() {
        Map<String, Integer> m = new LinkedCaseInsensitiveMap<>();
        m.put("B", 2);
        m.put("a", 1);
        assertEquals(List.of("B", "a"), new ArrayList<>(m.keySet()));
        assertEquals(2, m.get("b"));
        assertEquals(1, m.get("A"));
    }

    @Test
    void getAndPut_ignoreCase_lastSpellingWinsOrderUpdated() {
        Map<String, Integer> m = new LinkedCaseInsensitiveMap<>();
        m.put("Aa", 1);
        m.put("AA", 2);
        assertEquals(1, m.size());
        assertEquals(2, m.get("aa"));
        assertEquals(List.of("AA"), new ArrayList<>(m.keySet()));
    }

    @Test
    void getOrDefault_nullValueReturnsNullWhenKeyPresent() {
        Map<String, Integer> m = new LinkedCaseInsensitiveMap<>();
        m.put("K", null);
        assertNull(m.getOrDefault("k", -1));
        assertEquals(-1, m.getOrDefault("missing", -1));
    }

    @Test
    void computeIfAbsent_nullResult_doesNotExposeLogicalPresence() {
        Map<String, Integer> m = new LinkedCaseInsensitiveMap<>();
        assertNull(m.computeIfAbsent("x", k -> null));
        assertFalse(m.containsKey("X"));
    }

    @Test
    void compute_andMerge_differentSpelling_keepSingleCanonicalKey() {
        Map<String, Integer> m = new LinkedCaseInsensitiveMap<>();

        assertEquals(1, m.compute("Hello", (k, v) -> v == null ? 1 : v + 1));
        assertEquals(2, m.compute("HELLO", (k, v) -> v + 1));
        assertEquals(Set.of("Hello"), m.keySet());

        assertEquals(4, m.merge("hELLo", 2, Integer::sum));
        assertEquals(1, m.size());
        assertEquals(Set.of("Hello"), m.keySet());
        assertEquals(4, m.get("hello"));
    }

    @Test
    void nullKey_behavesLikeLinkedHashMap() {
        Map<String, Integer> m = new LinkedCaseInsensitiveMap<>();
        assertNull(m.put(null, 1));
        assertTrue(m.containsKey(null));
        assertEquals(1, m.get(null));
        assertEquals(1, m.getOrDefault(null, 0));
        assertEquals(1, m.remove(null));
        assertFalse(m.containsKey(null));
    }

    @Test
    void nullKey_nullValue_getOrDefaultReturnsNull() {
        Map<String, Integer> m = new LinkedCaseInsensitiveMap<>();
        m.put(null, null);
        assertNull(m.getOrDefault(null, 0));
    }

    @Test
    void keySetIteratorRemove_updatesCaseInsensitiveIndex() {
        Map<String, Integer> m = new LinkedCaseInsensitiveMap<>();
        m.put("A", 1);
        Iterator<String> it = m.keySet().iterator();
        assertEquals("A", it.next());
        it.remove();
        assertTrue(m.isEmpty());
        assertFalse(m.containsKey("a"));
    }

    @Test
    void valuesIteratorRemove_syncsIndex() {
        Map<String, Integer> m = new LinkedCaseInsensitiveMap<>();
        m.put("K", 1);
        Iterator<Integer> it = m.values().iterator();
        assertEquals(1, it.next());
        it.remove();
        assertTrue(m.isEmpty());
        assertFalse(m.containsKey("k"));
    }

    @Test
    void entrySetIteratorRemove_andClear() {
        Map<String, Integer> m = new LinkedCaseInsensitiveMap<>();
        m.put("A", 1);
        Iterator<Map.Entry<String, Integer>> it = m.entrySet().iterator();
        it.next();
        it.remove();
        assertTrue(m.isEmpty());
        m.put("x", 2);
        m.entrySet().clear();
        assertTrue(m.isEmpty());
    }

    @Test
    void entrySet_containsAndRemove_caseInsensitive() {
        Map<String, Integer> m = new LinkedCaseInsensitiveMap<>();
        m.put("Ab", 1);
        assertTrue(m.entrySet().contains(Map.entry("AB", 1)));
        assertTrue(m.entrySet().remove(Map.entry("ab", 1)));
        assertTrue(m.isEmpty());
    }

    @Test
    void putIfAbsent_skipsWhenLogicalKeyExists() {
        Map<String, Integer> m = new LinkedCaseInsensitiveMap<>();
        assertNull(m.putIfAbsent("a", 1));
        assertEquals(1, m.putIfAbsent("A", 2));
        assertEquals(1, m.get("a"));
    }

    @Test
    void removeKeyValue_caseInsensitive() {
        Map<String, Integer> m = new LinkedCaseInsensitiveMap<>();
        m.put("A", 1);
        assertFalse(m.remove("a", 999));
        assertTrue(m.remove("a", 1));
        assertTrue(m.isEmpty());
    }

    @Test
    void replace_caseInsensitive() {
        Map<String, Integer> m = new LinkedCaseInsensitiveMap<>();
        m.put("A", 1);
        assertEquals(1, m.replace("a", 2));
        assertTrue(m.replace("A", 2, 3));
        assertEquals(3, m.get("a"));
    }

    @Test
    void computeIfPresent_removesEntry() {
        Map<String, Integer> m = new LinkedCaseInsensitiveMap<>();
        m.put("X", 1);
        assertNull(m.computeIfPresent("x", (k, v) -> null));
        assertFalse(m.containsKey("X"));
    }

    @Test
    void forEach_visitsAllEntries() {
        Map<String, Integer> m = new LinkedCaseInsensitiveMap<>();
        m.put("a", 1);
        m.put("b", 2);
        AtomicInteger sum = new AtomicInteger();
        m.forEach((k, v) -> sum.addAndGet(v));
        assertEquals(3, sum.get());
    }

    @Test
    void keySetViewsAreCached() {
        Map<String, Integer> m = new LinkedCaseInsensitiveMap<>();
        assertSame(m.keySet(), m.keySet());
        assertSame(m.values(), m.values());
        assertSame(m.entrySet(), m.entrySet());
    }

    @Test
    void cloneIndependent() {
        LinkedCaseInsensitiveMap<Integer> m = new LinkedCaseInsensitiveMap<>();
        m.put("A", 1);
        LinkedCaseInsensitiveMap<Integer> c = m.clone();
        c.put("a", 2);
        assertEquals(1, m.get("a"));
        assertEquals(2, c.get("a"));
    }

    @Test
    void equals_hashCode_withHashMap() {
        Map<String, Integer> m = new LinkedCaseInsensitiveMap<>();
        m.put("A", 1);
        Map<String, Integer> h = new HashMap<>();
        h.put("A", 1);
        assertEquals(h, m);
        assertEquals(m, h);
        assertEquals(h.hashCode(), m.hashCode());
    }

    @Test
    void equals_differsWhenValuesDiffer() {
        Map<String, Integer> a = new LinkedCaseInsensitiveMap<>();
        a.put("a", 1);
        Map<String, Integer> b = new LinkedCaseInsensitiveMap<>();
        b.put("A", 2);
        assertNotEquals(a, b);
    }

    @Test
    void accessOrderConstructor_updatesOnGet() {
        LinkedCaseInsensitiveMap<Integer> m = new LinkedCaseInsensitiveMap<>(16, 0.75f, Locale.ROOT, true);
        m.put("a", 1);
        m.put("b", 2);
        m.get("a");
        assertEquals(List.of("b", "a"), new ArrayList<>(m.keySet()));
    }

    @Test
    void putAll_empty_noOp() {
        Map<String, Integer> m = new LinkedCaseInsensitiveMap<>();
        m.put("a", 1);
        m.putAll(Map.of());
        assertEquals(1, m.size());
    }

    @Test
    void computeIfAbsent_requiresNonNullFunction() {
        Map<String, Integer> m = new LinkedCaseInsensitiveMap<>();
        assertThrows(NullPointerException.class, () -> m.computeIfAbsent("a", null));
    }
}
