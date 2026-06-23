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
     * Splits {@code items} into exactly {@code parts} consecutive chunks of nearly equal size.
     * <p>Chunk size is {@code ceil(items.size() / parts)}. When there are fewer elements than
     * {@code parts}, trailing chunks are empty lists. The returned lists are independent copies
     * and do not reflect later changes to {@code items}.</p>
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
        List<List<T>> chunks = new ArrayList<>(splitIntoChunksBySize(items, chunkSize));
        while (chunks.size() < parts) {
            chunks.add(new ArrayList<>());
        }
        return Collections.unmodifiableList(chunks);
    }

    /**
     * Splits {@code items} into consecutive chunks of at most {@code chunkSize} elements.
     * <p>The last chunk may be smaller. For example, 120 items with {@code chunkSize = 100}
     * produce two chunks of sizes 100 and 20.</p>
     *
     * @param items     source list; {@code null} or empty, or non-positive {@code chunkSize}, yields an empty list
     * @param chunkSize maximum number of elements per chunk
     * @param <T>       element type
     * @return list of chunks in source order
     */
    public static <T> List<List<T>> splitIntoChunksBySize(List<T> items, int chunkSize) {
        if (items == null || items.isEmpty() || chunkSize <= 0) {
            return Collections.emptyList();
        }

        List<List<T>> chunks = new ArrayList<>();
        for (int index = 0; index < items.size(); index += chunkSize) {
            chunks.add(new ArrayList<>(items.subList(index, Math.min(index + chunkSize, items.size()))));
        }
        return Collections.unmodifiableList(chunks);
    }
}
