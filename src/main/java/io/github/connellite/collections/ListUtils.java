package io.github.connellite.collections;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * List helpers.
 */
@UtilityClass
public class ListUtils {

    /**
     * Splits {@code items} into at most {@code parts} consecutive chunks of nearly equal size.
     * <p>Chunk size is {@code ceil(items.size() / parts)}. The returned lists are independent
     * copies and do not reflect later changes to {@code items}.</p>
     *
     * @param items source list; {@code null} or empty, or non-positive {@code parts}, yields an empty list
     * @param parts desired number of chunks
     * @param <T>   element type
     * @return list of chunks in source order
     */
    public static <T> List<List<T>> splitIntoChunks(List<T> items, int parts) {
        if (items == null || items.isEmpty() || parts <= 0) {
            return Collections.emptyList();
        }

        int chunkSize = (int) Math.ceil((double) items.size() / parts);
        List<List<T>> chunks = new ArrayList<>();
        for (int index = 0; index < items.size(); index += chunkSize) {
            chunks.add(new ArrayList<>(items.subList(index, Math.min(index + chunkSize, items.size()))));
        }
        return chunks;
    }
}
