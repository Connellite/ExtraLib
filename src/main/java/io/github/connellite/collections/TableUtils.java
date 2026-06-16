package io.github.connellite.collections;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utilities for transposing column-oriented and row-oriented tabular data represented as maps.
 */
@UtilityClass
public class TableUtils {

    /**
     * Transposes {@code Map&lt;columnKey, columnValues&gt;} into a list of row maps.
     * <p>Each map entry is treated as a column. Row {@code i} contains the {@code i}-th element from every
     * column (by iteration order within each collection). Columns shorter than the longest column are padded
     * with {@code null}.</p>
     *
     * @param columns column key to values; {@code null} or empty yields an empty list
     * @param <K>     column key type
     * @param <T>     cell value type
     * @return one map per row; keys are column keys and iteration order follows {@code columns}
     */
    public static <K, T> List<Map<K, T>> transposeColumnsToRows(Map<K, ? extends Collection<T>> columns) {
        if (columns == null || columns.isEmpty()) {
            return Collections.emptyList();
        }

        int maxRows = columns.values().stream()
                .mapToInt(Collection::size)
                .max()
                .orElse(0);

        Map<K, List<T>> indexedColumns = new LinkedHashMap<>();
        columns.forEach((key, value) -> indexedColumns.put(key, new ArrayList<>(value)));

        List<Map<K, T>> rows = new ArrayList<>(maxRows);
        for (int rowIndex = 0; rowIndex < maxRows; rowIndex++) {
            Map<K, T> row = new LinkedHashMap<>();
            for (Map.Entry<K, List<T>> entry : indexedColumns.entrySet()) {
                List<T> values = entry.getValue();
                row.put(entry.getKey(), rowIndex < values.size() ? values.get(rowIndex) : null);
            }
            rows.add(row);
        }
        return rows;
    }

    /**
     * Transposes a list of row maps into {@code Map&lt;columnKey, columnValues&gt;}.
     * <p>Each distinct column key collects non-{@code null} cell values in row order.
     * {@code null} rows are skipped.</p>
     *
     * @param rows row maps; {@code null} or empty yields an empty map
     * @param <K>  column key type
     * @param <T>  cell value type
     * @return column key to ordered set of values
     */
    public static <K, T> Map<K, Set<T>> transposeRowsToColumns(List<Map<K, T>> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<K, Set<T>> columns = new LinkedHashMap<>();
        for (Map<K, T> row : rows) {
            if (row == null) {
                continue;
            }
            for (Map.Entry<K, T> entry : row.entrySet()) {
                T value = entry.getValue();
                if (value != null) {
                    columns.computeIfAbsent(entry.getKey(), key -> new LinkedHashSet<>()).add(value);
                }
            }
        }
        return columns;
    }
}
