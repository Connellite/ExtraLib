package io.github.connellite.collections;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NullSkippingCollectionTest {

    @Test
    void arrayListSkipsNullOnAdd() {
        NullSkippingArrayList<String> list = new NullSkippingArrayList<>();
        assertFalse(list.add(null));
        list.add("a");
        assertEquals(1, list.size());
        list.addAll(new ArrayList<>() {{
            add("b");
            add(null);
            add("c");
        }});
        assertEquals(List.of("a", "b", "c"), list);
    }

    @Test
    void hashSetSkipsNull() {
        NullSkippingHashSet<String> set = new NullSkippingHashSet<>();
        assertFalse(set.add(null));
        set.add("x");
        assertTrue(set.contains("x"));
        assertEquals(1, set.size());
    }

    @Test
    void hashMapSkipsNullKey() {
        NullSkippingHashMap<String, String> map = new NullSkippingHashMap<>();
        assertNull(map.put(null, "v"));
        map.put("k", "v");
        assertEquals("v", map.get("k"));
        assertEquals(1, map.size());
    }
}
