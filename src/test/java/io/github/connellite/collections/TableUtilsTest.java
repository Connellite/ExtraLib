package io.github.connellite.collections;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableUtilsTest {

    @Test
    void transposeColumnsToRows_nullOrEmptyReturnsEmptyList() {
        assertTrue(TableUtils.transposeColumnsToRows(null).isEmpty());
        assertTrue(TableUtils.transposeColumnsToRows(Map.of()).isEmpty());
    }

    @Test
    void transposeColumnsToRows_buildsRowsWithNullPadding() {
        Map<String, List<String>> columns = new LinkedHashMap<>();
        columns.put("a", List.of("a1", "a2"));
        columns.put("b", List.of("b1"));

        List<Map<String, String>> rows = TableUtils.transposeColumnsToRows(columns);

        assertEquals(2, rows.size());
        assertEquals("a1", rows.get(0).get("a"));
        assertEquals("b1", rows.get(0).get("b"));
        assertEquals("a2", rows.get(1).get("a"));
        assertNull(rows.get(1).get("b"));
    }

    @Test
    void transposeRowsToColumns_nullOrEmptyReturnsEmptyMap() {
        assertTrue(TableUtils.transposeRowsToColumns(null).isEmpty());
        assertTrue(TableUtils.transposeRowsToColumns(List.of()).isEmpty());
    }

    @Test
    void transposeRowsToColumns_skipsNullRowsAndNullCells() {
        Map<String, String> first = Map.of("x", "1");
        Map<String, String> second = new LinkedHashMap<>();
        second.put("x", null);
        second.put("y", "2");
        List<Map<String, String>> rows = new ArrayList<>();
        rows.add(first);
        rows.add(null);
        rows.add(second);

        Map<String, Set<String>> columns = TableUtils.transposeRowsToColumns(rows);

        assertEquals(Set.of("1"), columns.get("x"));
        assertEquals(Set.of("2"), columns.get("y"));
    }

    @Test
    void transposeColumnsToRowsAndBack_roundTripsUuidColumns() {
        UUID first = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID second = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");

        Map<String, Set<UUID>> original = new LinkedHashMap<>();
        original.put("docs", new LinkedHashSet<>(List.of(first, second)));
        original.put("tags", new LinkedHashSet<>(List.of(second)));

        List<Map<String, UUID>> rows = TableUtils.transposeColumnsToRows(original);
        Map<String, Set<UUID>> restored = TableUtils.transposeRowsToColumns(rows);

        assertEquals(original, restored);
    }

    @Test
    void transposeRowsToColumns_preservesInsertionOrderWithinColumn() {
        List<Map<String, Integer>> rows = List.of(
                Map.of("k", 1),
                Map.of("k", 2),
                Map.of("k", 3)
        );

        Set<Integer> column = TableUtils.transposeRowsToColumns(rows).get("k");

        assertEquals(List.of(1, 2, 3), List.copyOf(column));
    }

    @Test
    void transposeColumnsToRows_supportsNonStringColumnKeys() {
        Map<Integer, List<String>> columns = new LinkedHashMap<>();
        columns.put(1, List.of("a"));
        columns.put(2, List.of("b", "c"));

        List<Map<Integer, String>> rows = TableUtils.transposeColumnsToRows(columns);

        assertEquals("a", rows.get(0).get(1));
        assertEquals("b", rows.get(0).get(2));
        assertNull(rows.get(1).get(1));
        assertEquals("c", rows.get(1).get(2));
    }
}
