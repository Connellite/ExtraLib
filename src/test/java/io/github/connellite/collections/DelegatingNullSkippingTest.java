package io.github.connellite.collections;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void collectionNullDelegateRejected() {
        assertThrows(NullPointerException.class, () -> new DelegatingNullSkippingCollection<String>(null));
    }

    @Test
    void collectionAddAllOnlyNullsReturnsFalse() {
        List<String> inner = new ArrayList<>(List.of("x"));
        DelegatingNullSkippingCollection<String> wrap = new DelegatingNullSkippingCollection<>(inner);
        assertFalse(wrap.addAll(Arrays.asList(null, null)));
        assertEquals(List.of("x"), inner);
    }

    @Test
    void collectionEqualsDelegatesToInnerOrOtherWrapper() {
        List<String> inner = new ArrayList<>(List.of("a"));
        DelegatingNullSkippingCollection<String> wrap = new DelegatingNullSkippingCollection<>(inner);
        assertEquals(wrap, inner);
        assertEquals(wrap, new DelegatingNullSkippingCollection<>(inner));
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

    @Test
    void mapNullDelegateRejected() {
        assertThrows(NullPointerException.class, () -> new DelegatingNullSkippingMap<String, String>(null));
    }

    @Test
    void mapPutAllSkipsNullKeys() {
        Map<String, Integer> inner = new HashMap<>();
        DelegatingNullSkippingMap<String, Integer> wrap = new DelegatingNullSkippingMap<>(inner);
        Map<String, Integer> src = new LinkedHashMap<>();
        src.put("a", 1);
        src.put(null, 9);
        wrap.putAll(src);
        assertEquals(Map.of("a", 1), inner);
    }

    @Test
    void mapMergePutIfAbsentComputeSkipNullKey() {
        Map<String, Integer> inner = new HashMap<>();
        inner.put("x", 1);
        DelegatingNullSkippingMap<String, Integer> wrap = new DelegatingNullSkippingMap<>(inner);
        assertNull(wrap.merge(null, 99, Integer::sum));
        assertNull(wrap.putIfAbsent(null, 2));
        assertNull(wrap.computeIfAbsent(null, k -> 3));
        assertNull(wrap.computeIfPresent(null, (k, v) -> 4));
        assertNull(wrap.compute(null, (k, v) -> 5));
        assertEquals(1, inner.get("x"));
        assertFalse(inner.containsKey(null));
    }

    @Test
    void mapRemoveAndReplaceNullKeyNoOp() {
        Map<String, Integer> inner = new HashMap<>();
        inner.put("a", 1);
        DelegatingNullSkippingMap<String, Integer> wrap = new DelegatingNullSkippingMap<>(inner);
        assertFalse(wrap.remove(null, 1));
        assertNull(wrap.replace(null, 2));
        assertFalse(wrap.replace(null, 1, 2));
        assertEquals(Map.of("a", 1), inner);
    }

    @Test
    void mapMergeRequiresNonNullRemappingFunction() {
        Map<String, Integer> inner = new HashMap<>();
        DelegatingNullSkippingMap<String, Integer> wrap = new DelegatingNullSkippingMap<>(inner);
        assertThrows(NullPointerException.class, () -> wrap.merge("a", 1, null));
    }

    @Test
    void mapEqualsDelegates() {
        Map<String, Integer> inner = Map.of("a", 1);
        DelegatingNullSkippingMap<String, Integer> wrap = new DelegatingNullSkippingMap<>(new HashMap<>(inner));
        assertEquals(inner, wrap);
        assertEquals(wrap, inner);
    }
}
