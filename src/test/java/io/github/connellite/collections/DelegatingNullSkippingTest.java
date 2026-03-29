package io.github.connellite.collections;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DelegatingNullSkippingTest {

    @Test
    void collectionDelegatesAndSkipsNull() {
        List<String> inner = new ArrayList<>();
        DelegatingNullSkippingCollection<String> wrap = new DelegatingNullSkippingCollection<>(inner);
        assertFalse(wrap.add(null));
        wrap.add("a");
        assertEquals(List.of("a"), inner);
        wrap.addAll(new ArrayList<>() {{
            add(null);
            add("b");
        }});
        assertEquals(List.of("a", "b"), inner);
        assertEquals(inner, wrap.delegate());
    }

    @Test
    void mapDelegatesAndSkipsNullKey() {
        Map<String, String> inner = new HashMap<>();
        DelegatingNullSkippingMap<String, String> wrap = new DelegatingNullSkippingMap<>(inner);
        assertNull(wrap.put(null, "x"));
        wrap.put("k", "v");
        assertEquals(Map.of("k", "v"), inner);
        assertTrue(wrap.delegate().containsKey("k"));
    }
}
