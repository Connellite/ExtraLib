package io.github.connellite.collections;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

class MultiValueHashMapTest {

    @Test
    void multiValueHashMap_basicOperations() {
        MultiValueHashMap<String, Integer> map = new MultiValueHashMap<>();
        map.add("a", 1);
        map.add("a", 2);

        assertEquals(1, map.getFirst("a"));
        assertEquals(List.of(1, 2), map.get("a"));

        map.set("a", 9);
        assertEquals(List.of(9), map.get("a"));

        map.setAll(Map.of("b", 3));
        assertEquals(3, map.getFirst("b"));

        Map<String, Integer> single = map.toSingleValueMap();
        assertEquals(Map.of("a", 9, "b", 3), single);
    }

    @Test
    void multiValueHashMap_addIfAbsentAndDeepCopy() {
        MultiValueHashMap<String, String> map = new MultiValueHashMap<>();
        map.addIfAbsent("k", "v1");
        map.addIfAbsent("k", "v2");

        assertEquals(List.of("v1"), map.get("k"));
        assertNull(map.getFirst("missing"));

        MultiValueHashMap<String, String> copy = map.deepCopy();
        assertNotSame(map, copy);
        assertNotSame(map.get("k"), copy.get("k"));

        copy.add("k", "v3");
        assertEquals(List.of("v1"), map.get("k"));
        assertEquals(List.of("v1", "v3"), copy.get("k"));
    }

    @Test
    void linkedMultiValueHashMap_basicOperations() {
        LinkedMultiValueHashMap<String, Integer> map = new LinkedMultiValueHashMap<>();
        map.add("x", 10);
        map.add("x", 11);
        map.add("y", 20);

        assertEquals(10, map.getFirst("x"));
        assertEquals(List.of(10, 11), map.get("x"));
        assertEquals(Map.of("x", 10, "y", 20), map.toSingleValueMap());
    }

    @Test
    void linkedMultiValueHashMap_deepCopyIndependentLists() {
        LinkedMultiValueHashMap<String, String> map = new LinkedMultiValueHashMap<>();
        map.add("k", "a");

        LinkedMultiValueHashMap<String, String> copy = map.deepCopy();
        assertNotSame(map, copy);
        assertNotSame(map.get("k"), copy.get("k"));

        copy.add("k", "b");
        assertEquals(List.of("a"), map.get("k"));
        assertEquals(List.of("a", "b"), copy.get("k"));
    }
}
