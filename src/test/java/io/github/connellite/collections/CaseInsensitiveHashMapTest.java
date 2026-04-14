package io.github.connellite.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class CaseInsensitiveHashMapTest {

    @Test
    void getAndPut_ignoreCase_preservesLastSpelling() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        assertNull(m.put("Foo", 1));
        assertEquals(1, m.get("foo"));
        assertEquals(1, m.get("FOO"));
        assertEquals(1, m.put("fOo", 2));
        assertEquals(2, m.get("FOO"));
        assertTrue(m.containsKey("foo"));
        Set<String> keys = m.keySet();
        assertEquals(1, keys.size());
        assertEquals("fOo", keys.iterator().next());
    }

    @Test
    void getOrDefault_caseInsensitiveAndMissing() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        assertEquals(-1, m.getOrDefault("x", -1));
        m.put("Ab", 1);
        assertEquals(1, m.getOrDefault("AB", -1));
        assertEquals(-1, m.getOrDefault("none", -1));
    }

    @Test
    void getOrDefault_nullKey() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        assertEquals(0, m.getOrDefault(null, 0));
        m.put(null, 5);
        assertEquals(5, m.getOrDefault(null, 0));
    }

    @Test
    void containsKey_caseInsensitive_falseForUnknownAndNonString() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        assertFalse(m.containsKey("a"));
        m.put("A", 1);
        assertTrue(m.containsKey("a"));
        assertFalse(m.containsKey(1));
    }

    @Test
    void get_nonStringKey_returnsNull() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        m.put("a", 1);
        assertNull(m.get(1));
    }

    @Test
    void putAll_putPerEntry_caseInsensitive() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        m.put("X", 1);
        Map<String, Integer> src = new HashMap<>();
        src.put("x", 2);
        src.put("y", 3);
        m.putAll(src);
        assertEquals(2, m.size());
        assertEquals(2, m.get("X"));
        assertEquals(3, m.get("Y"));
    }

    @Test
    void putAll_empty_noOp() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        m.put("a", 1);
        m.putAll(Map.of());
        assertEquals(1, m.size());
    }

    @Test
    void constructor_copiesMap() {
        Map<String, Integer> src = new HashMap<>();
        src.put("a", 1);
        src.put("B", 2);
        Map<String, Integer> m = new CaseInsensitiveHashMap<>(src);
        assertEquals(2, m.size());
        assertEquals(1, m.get("A"));
        assertEquals(2, m.get("b"));
    }

    @Test
    void constructor_withLocale() {
        CaseInsensitiveHashMap<Integer> m = new CaseInsensitiveHashMap<>(Map.of("Z", 1), Locale.ROOT);
        assertEquals(Locale.ROOT, m.getLocale());
        assertEquals(1, m.get("z"));
    }

    @Test
    void clear_emptiesCaseIndex() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        m.put("a", 1);
        m.clear();
        assertTrue(m.isEmpty());
        assertFalse(m.containsKey("A"));
        assertNull(m.get("a"));
    }

    @Test
    void values_clear() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        m.put("a", 1);
        m.values().clear();
        assertTrue(m.isEmpty());
    }

    @Test
    void values_iteratorRemove_syncsIndex() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        m.put("K", 1);
        Iterator<Integer> it = m.values().iterator();
        assertEquals(1, it.next());
        it.remove();
        assertTrue(m.isEmpty());
        assertFalse(m.containsKey("k"));
    }

    @Test
    void remove_updatesIndex() {
        Map<String, String> m = new CaseInsensitiveHashMap<>();
        m.put("A", "x");
        assertEquals("x", m.remove("a"));
        assertTrue(m.isEmpty());
        assertFalse(m.containsKey("A"));
    }

    @Test
    void removeKeyValue_caseInsensitive() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        m.put("A", 1);
        assertFalse(m.remove("a", 999));
        assertTrue(m.containsKey("a"));
        assertTrue(m.remove("a", 1));
        assertFalse(m.containsKey("A"));
    }

    @Test
    void removeKeyValue_nullKey() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        m.put(null, 1);
        assertTrue(m.remove(null, 1));
        assertFalse(m.containsKey(null));
    }

    @Test
    void keySetIteratorRemove_syncsIndex() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        m.put("K", 1);
        Iterator<String> it = m.keySet().iterator();
        assertTrue(it.hasNext());
        assertEquals("K", it.next());
        it.remove();
        assertTrue(m.isEmpty());
        assertFalse(m.containsKey("k"));
    }

    @Test
    void entrySetIteratorRemove_syncsIndex() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        m.put("K", 1);
        Iterator<Map.Entry<String, Integer>> it = m.entrySet().iterator();
        it.next();
        it.remove();
        assertTrue(m.isEmpty());
    }

    @Test
    void entrySet_contains_caseInsensitiveKeyMatch() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        m.put("Ab", 1);
        assertTrue(m.entrySet().contains(Map.entry("AB", 1)));
        assertFalse(m.entrySet().contains(Map.entry("AB", 2)));
    }

    @Test
    void entrySet_remove_caseInsensitive() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        m.put("Ab", 1);
        assertTrue(m.entrySet().remove(Map.entry("ab", 1)));
        assertTrue(m.isEmpty());
    }

    @Test
    void putIfAbsent_insertsThenSkipsWhenSameLogicalKey() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        assertNull(m.putIfAbsent("a", 1));
        assertEquals(1, m.putIfAbsent("A", 2));
        assertEquals(1, m.get("a"));
    }

    @Test
    void putIfAbsent_nullKey() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        assertNull(m.putIfAbsent(null, 1));
        assertEquals(1, m.putIfAbsent(null, 2));
        assertEquals(1, m.get(null));
    }

    @Test
    void computeIfAbsent_doesNotIndexWhenAbsentAndNull() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        assertNull(m.computeIfAbsent("x", k -> null));
        assertFalse(m.containsKey("x"));
    }

    @Test
    void computeIfAbsent_existingLogicalKey_skipsFactory() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        m.put("Only", 5);
        assertEquals(5, m.computeIfAbsent("ONLY", k -> {
            throw new AssertionError("should not run");
        }));
    }

    @Test
    void computeIfAbsent_nullKey() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        assertEquals(7, m.computeIfAbsent(null, k -> 7));
        assertEquals(7, m.computeIfAbsent(null, k -> 99));
    }

    @Test
    void computeIfAbsent_requiresNonNullFunction() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        assertThrows(NullPointerException.class, () -> m.computeIfAbsent("a", null));
    }

    @Test
    void computeIfPresent_updatesAndRemoves() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        assertNull(m.computeIfPresent("x", (k, v) -> v + 1));
        m.put("X", 1);
        assertEquals(2, m.computeIfPresent("x", (k, v) -> v + 1));
        assertEquals(2, m.get("X"));
        assertNull(m.computeIfPresent("x", (k, v) -> null));
        assertFalse(m.containsKey("X"));
    }

    @Test
    void computeIfPresent_nullKey() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        m.put(null, 1);
        assertEquals(2, m.computeIfPresent(null, (k, v) -> v + 1));
    }

    @Test
    void compute_addUpdateRemove() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        assertEquals(10, m.compute("Ab", (k, v) -> v == null ? 10 : v));
        assertEquals(10, m.get("ab"));
        assertEquals(11, m.compute("AB", (k, v) -> v + 1));
        assertNull(m.compute("aB", (k, v) -> null));
        assertFalse(m.containsKey("Ab"));
    }

    @Test
    void compute_absentThenDifferentSpelling_keepsSingleCanonicalKey() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        assertEquals(1, m.compute("Hello", (k, v) -> v == null ? 1 : v + 1));
        assertEquals(2, m.compute("HELLO", (k, v) -> v + 1));
        assertEquals(1, m.size());
        assertEquals(Set.of("Hello"), m.keySet());
        assertEquals(2, m.get("hello"));
    }

    @Test
    void compute_nullKey() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        assertEquals(3, m.compute(null, (k, v) -> v == null ? 3 : v + 10));
        assertEquals(13, m.compute(null, (k, v) -> v + 10));
    }

    @Test
    void merge_absent_present_removal() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        assertEquals(1, m.merge("a", 1, Integer::sum));
        assertEquals(1, m.get("A"));
        assertEquals(3, m.merge("A", 2, Integer::sum));
        assertNull(m.merge("a", 0, (old, val) -> null));
        assertFalse(m.containsKey("a"));
    }

    @Test
    void merge_absentThenDifferentSpelling_keepsSingleCanonicalKey() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        assertEquals(1, m.merge("Hello", 1, Integer::sum));
        assertEquals(3, m.merge("HELLO", 2, Integer::sum));
        assertEquals(1, m.size());
        assertEquals(Set.of("Hello"), m.keySet());
        assertEquals(3, m.get("hello"));
    }

    @Test
    void merge_nullKey() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        assertEquals(1, m.merge(null, 1, Integer::sum));
        assertEquals(3, m.merge(null, 2, Integer::sum));
    }

    @Test
    void merge_requiresNonNullValue() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        assertThrows(NullPointerException.class, () -> m.merge("a", null, (x, y) -> x));
    }

    @Test
    void forEach_visitsEntries() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        m.put("a", 1);
        AtomicInteger sum = new AtomicInteger();
        m.forEach((k, v) -> sum.addAndGet(v));
        assertEquals(1, sum.get());
    }

    @Test
    void equals_hashCode_withHashMap() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        m.put("A", 1);
        Map<String, Integer> h = new HashMap<>();
        h.put("A", 1);
        assertEquals(h, m);
        assertEquals(m, h);
        assertEquals(h.hashCode(), m.hashCode());
    }

    @Test
    void equals_differsWhenValuesDiffer() {
        Map<String, Integer> a = new CaseInsensitiveHashMap<>();
        a.put("a", 1);
        Map<String, Integer> b = new CaseInsensitiveHashMap<>();
        b.put("A", 2);
        assertNotEquals(a, b);
    }

    @Test
    void keySet_values_entrySet_cachedInstances() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        assertSame(m.keySet(), m.keySet());
        assertSame(m.values(), m.values());
        assertSame(m.entrySet(), m.entrySet());
    }

    @Test
    void nullKey_likeHashMap() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        assertNull(m.put(null, 1));
        assertTrue(m.containsKey(null));
        assertEquals(1, m.get(null));
        assertEquals(1, m.remove(null));
        assertFalse(m.containsKey(null));
    }

    @Test
    void keySetRemove_trueWhenValueIsNull() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        m.put("k", null);
        assertTrue(m.keySet().remove("K"));
        assertTrue(m.isEmpty());
    }

    @Test
    void nullKey_entryIteratorRemove_doesNotTouchCaseIndex() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        m.put(null, 0);
        m.put("A", 1);
        Iterator<Map.Entry<String, Integer>> it = m.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            if (e.getKey() == null) {
                it.remove();
                break;
            }
        }
        assertFalse(m.containsKey(null));
        assertEquals(1, m.get("a"));
    }

    @Test
    void explicitLocaleUsedForNormalization() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>(Locale.ROOT);
        m.put("HTTP", 1);
        assertEquals(1, m.get("http"));
    }

    @Test
    void cloneIndependent() {
        CaseInsensitiveHashMap<Integer> m = new CaseInsensitiveHashMap<>();
        m.put("A", 1);
        Map<String, Integer> c = m.clone();
        c.put("a", 2);
        assertEquals(1, m.get("a"));
        assertEquals(2, c.get("a"));
    }

    @Test
    void replace_caseInsensitive_singleArg() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        m.put("A", 1);
        assertEquals(1, m.replace("a", 2));
        assertEquals(2, m.get("A"));
    }

    @Test
    void replace_caseInsensitive_oldAndNewValue() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        m.put("A", 1);
        assertTrue(m.replace("a", 1, 2));
        assertEquals(2, m.get("a"));
        assertFalse(m.replace("A", 1, 3));
        assertEquals(2, m.get("A"));
    }

    @Test
    void replace_nullKey_delegatesToSuper() {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();
        m.put(null, 1);
        assertEquals(1, m.replace(null, 2));
        assertTrue(m.replace(null, 2, 3));
        assertEquals(3, m.get(null));
    }
}
