package io.github.connellite.collections;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertEquals(1, m.remove(null));
        assertFalse(m.containsKey(null));
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
}
