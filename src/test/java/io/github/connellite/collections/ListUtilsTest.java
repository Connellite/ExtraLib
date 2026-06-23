package io.github.connellite.collections;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListUtilsTest {

    @Test
    void splitIntoChunks_nullOrEmptyOrNonPositivePartsReturnsEmptyList() {
        assertTrue(ListUtils.splitIntoChunks(null, 3).isEmpty());
        assertTrue(ListUtils.splitIntoChunks(List.of(), 3).isEmpty());
        assertTrue(ListUtils.splitIntoChunks(List.of(1, 2), 0).isEmpty());
        assertTrue(ListUtils.splitIntoChunks(List.of(1, 2), -1).isEmpty());
    }

    @Test
    void splitIntoChunks_dividesIntoNearlyEqualParts() {
        List<Integer> source = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        List<List<Integer>> chunks = ListUtils.splitIntoChunks(source, 3);

        assertEquals(List.of(
                List.of(1, 2, 3, 4),
                List.of(5, 6, 7, 8),
                List.of(9, 10)
        ), chunks);
    }

    @Test
    void splitIntoChunks_singlePartReturnsOneChunk() {
        List<String> source = List.of("a", "b", "c");

        assertEquals(List.of(source), ListUtils.splitIntoChunks(source, 1));
    }

    @Test
    void splitIntoChunks_morePartsThanItemsPadsWithEmptyChunks() {
        List<Integer> source = List.of(1, 2, 3);

        assertEquals(List.of(List.of(1), List.of(2), List.of(3), List.of(), List.of()),
                ListUtils.splitIntoChunks(source, 5));
    }

    @Test
    void splitIntoChunks_fewerItemsThanPartsReturnsExactPartCount() {
        List<Integer> source = List.of(1, 2, 3, 4, 5);

        List<List<Integer>> chunks = ListUtils.splitIntoChunks(source, 10);

        assertEquals(10, chunks.size());
        assertEquals(List.of(1), chunks.get(0));
        assertEquals(List.of(5), chunks.get(4));
        for (int i = 5; i < 10; i++) {
            assertTrue(chunks.get(i).isEmpty(), "chunk " + i + " should be empty");
        }
    }

    @Test
    void splitIntoChunks_returnsIndependentCopies() {
        List<Integer> source = new ArrayList<>(List.of(1, 2, 3, 4));

        List<List<Integer>> chunks = ListUtils.splitIntoChunks(source, 2);
        source.set(0, 99);
        chunks.get(0).set(0, 42);

        assertEquals(List.of(99, 2, 3, 4), source);
        assertEquals(List.of(42, 2), chunks.get(0));
    }

    @Test
    void splitIntoChunks_worksWithUuidList() {
        UUID first = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID second = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");
        UUID third = UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8");
        List<UUID> docs = List.of(first, second, third);

        List<List<UUID>> chunks = ListUtils.splitIntoChunks(docs, 2);

        assertEquals(2, chunks.size());
        assertEquals(List.of(first, second), chunks.get(0));
        assertEquals(List.of(third), chunks.get(1));
        assertNotSame(docs, chunks.get(0));
    }

    @Test
    void splitIntoChunksBySize_nullOrEmptyOrNonPositiveChunkSizeReturnsEmptyList() {
        assertTrue(ListUtils.splitIntoChunksBySize(null, 100).isEmpty());
        assertTrue(ListUtils.splitIntoChunksBySize(List.of(), 100).isEmpty());
        assertTrue(ListUtils.splitIntoChunksBySize(List.of(1, 2), 0).isEmpty());
        assertTrue(ListUtils.splitIntoChunksBySize(List.of(1, 2), -1).isEmpty());
    }

    @Test
    void splitIntoChunksBySize_splitsIntoFixedSizeChunks() {
        List<Integer> source = new ArrayList<>();
        for (int i = 1; i <= 120; i++) {
            source.add(i);
        }

        List<List<Integer>> chunks = ListUtils.splitIntoChunksBySize(source, 100);

        assertEquals(2, chunks.size());
        assertEquals(100, chunks.get(0).size());
        assertEquals(20, chunks.get(1).size());
        assertEquals(1, chunks.get(0).get(0));
        assertEquals(100, chunks.get(0).get(99));
        assertEquals(101, chunks.get(1).get(0));
        assertEquals(120, chunks.get(1).get(19));
    }

    @Test
    void splitIntoChunksBySize_exactMultipleReturnsEqualChunks() {
        List<Integer> source = List.of(1, 2, 3, 4);

        assertEquals(List.of(List.of(1, 2), List.of(3, 4)), ListUtils.splitIntoChunksBySize(source, 2));
    }
}
