package io.github.connellite.jdbc;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NamedQueryTest {

    @Test
    void resolveExpandsCollectionIntoMultiplePlaceholders() {
        NamedQuery.ResolvedQuery resolved = NamedQuery.of("SELECT * FROM demo WHERE id IN (:ids) AND status = :status")
                .setCollection("ids", List.of(1, 2, 3))
                .setObject("status", "active")
                .resolve();

        assertEquals("SELECT * FROM demo WHERE id IN (?,?,?) AND status = ?", resolved.sql());
        assertEquals(List.of(1, 2, 3, "active"), resolved.values());
    }

    @Test
    void prepareExecutesExpandedCollectionQuery() throws Exception {
        try (Connection connection = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(connection);

            try (NamedPreparedStatement statement = NamedQuery.of(
                    "SELECT name FROM demo WHERE id IN (:ids) ORDER BY id")
                    .setCollection("ids", List.of(1, 2))
                    .prepare(connection);
                 ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals("one", resultSet.getString("name"));
                assertTrue(resultSet.next());
                assertEquals("two", resultSet.getString("name"));
                assertFalse(resultSet.next());
            }
        }
    }

    @Test
    void repeatedCollectionParameterExpandsEachOccurrence() {
        NamedQuery.ResolvedQuery resolved = NamedQuery.of("SELECT :id AS a, :id AS b")
                .setCollection("id", List.of(10, 20))
                .resolve();

        assertEquals("SELECT ?,? AS a, ?,? AS b", resolved.sql());
        assertEquals(List.of(10, 20, 10, 20), resolved.values());
    }

    @Test
    void emptyCollectionThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                NamedQuery.of("SELECT * FROM demo WHERE id IN (:ids)")
                        .setCollection("ids", List.of()));
    }

    @Test
    void prepareWithoutBindingThrows() {
        assertThrows(IllegalStateException.class, () ->
                NamedQuery.of("SELECT :x AS v").resolve());
    }

    @Test
    void unknownParameterThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                NamedQuery.of("SELECT :x AS v").setObject("missing", 1));
    }

    @Test
    void doubleBindThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                NamedQuery.of("SELECT :x AS v")
                        .setObject("x", 1)
                        .setObject("x", 2));
    }

    @Test
    void setAllBindsScalarParameters() {
        NamedQuery.ResolvedQuery resolved = NamedQuery.of("SELECT :a + :b AS s")
                .setAll(Map.of("a", 2, "b", 3))
                .resolve();

        assertEquals("SELECT ? + ? AS s", resolved.sql());
        assertEquals(List.of(2, 3), resolved.values());
    }

    @Test
    void resolveCollectionStringAndTwoCollections() {
        NamedQuery.ResolvedQuery resolved = NamedQuery.of(
                        "SELECT d.name FROM demo d "
                                + "JOIN child c ON c.demo_id = d.id "
                                + "WHERE d.id IN (:ids) AND d.name = :name AND c.cid IN (:childIds)")
                .setCollection("ids", List.of(1, 2))
                .setString("name", "one")
                .setCollection("childIds", List.of(1))
                .resolve();

        assertEquals(
                "SELECT d.name FROM demo d "
                        + "JOIN child c ON c.demo_id = d.id "
                        + "WHERE d.id IN (?,?) AND d.name = ? AND c.cid IN (?)",
                resolved.sql());
        assertEquals(List.of(1, 2, "one", 1), resolved.values());
    }

    @Test
    void resolveTwoCollectionsPreservesSqlParameterOrder() {
        NamedQuery.ResolvedQuery resolved = NamedQuery.of(
                        "SELECT tag FROM child WHERE demo_id IN (:demoIds) AND cid IN (:childIds)")
                .setCollection("childIds", List.of(1))
                .setCollection("demoIds", List.of(1, 2))
                .resolve();

        assertEquals("SELECT tag FROM child WHERE demo_id IN (?,?) AND cid IN (?)", resolved.sql());
        assertEquals(List.of(1, 2, 1), resolved.values());
    }

    @Test
    void prepareCollectionStringAndTwoCollections() throws Exception {
        try (Connection connection = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(connection);

            try (NamedPreparedStatement statement = NamedQuery.of(
                            "SELECT d.name, c.tag FROM demo d "
                                    + "JOIN child c ON c.demo_id = d.id "
                                    + "WHERE d.id IN (:ids) AND d.name = :name AND c.cid IN (:childIds) "
                                    + "ORDER BY d.id")
                    .setCollection("ids", List.of(1, 2))
                    .setString("name", "one")
                    .setCollection("childIds", List.of(1))
                    .prepare(connection);
                 ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals("one", resultSet.getString("name"));
                assertEquals("c", resultSet.getString("tag"));
                assertFalse(resultSet.next());
            }
        }
    }

    @Test
    void prepareTwoCollectionsFiltersByBothInClauses() throws Exception {
        try (Connection connection = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(connection);

            try (NamedPreparedStatement statement = NamedQuery.of(
                            "SELECT tag FROM child WHERE demo_id IN (:demoIds) AND cid IN (:childIds)")
                    .setCollection("demoIds", List.of(1, 2))
                    .setCollection("childIds", List.of(1))
                    .prepare(connection);
                 ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals("c", resultSet.getString("tag"));
                assertFalse(resultSet.next());
            }
        }
    }

    @Test
    void mixedBindingsCanBeReusedAfterPrepare() throws Exception {
        try (Connection connection = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(connection);

            NamedQuery query = NamedQuery.of(
                            "SELECT name FROM demo WHERE id IN (:ids) AND name = :name ORDER BY id")
                    .setString("name", "one")
                    .setCollection("ids", List.of(1, 2));

            NamedQuery.ResolvedQuery firstResolve = query.resolve();
            NamedQuery.ResolvedQuery secondResolve = query.resolve();
            assertEquals(
                    "SELECT name FROM demo WHERE id IN (?,?) AND name = ? ORDER BY id",
                    firstResolve.sql());
            assertEquals(List.of(1, 2, "one"), firstResolve.values());
            assertEquals(firstResolve, secondResolve);

            try (NamedPreparedStatement first = query.prepare(connection);
                 ResultSet firstResult = first.executeQuery()) {
                assertTrue(firstResult.next());
                assertEquals("one", firstResult.getString("name"));
                assertFalse(firstResult.next());
            }

            try (NamedPreparedStatement second = query.prepare(connection);
                 ResultSet secondResult = second.executeQuery()) {
                assertTrue(secondResult.next());
                assertEquals("one", secondResult.getString("name"));
                assertFalse(secondResult.next());
            }
        }
    }

    @Test
    void canPrepareAndResolveMultipleTimes() throws Exception {
        try (Connection connection = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(connection);

            NamedQuery query = NamedQuery.of("SELECT name FROM demo WHERE id = :id")
                    .setInt("id", 1);

            NamedQuery.ResolvedQuery firstResolve = query.resolve();
            NamedQuery.ResolvedQuery secondResolve = query.resolve();
            assertEquals("SELECT name FROM demo WHERE id = ?", firstResolve.sql());
            assertEquals(List.of(1), firstResolve.values());
            assertEquals(firstResolve, secondResolve);

            try (NamedPreparedStatement first = query.prepare(connection);
                 ResultSet firstResult = first.executeQuery()) {
                assertTrue(firstResult.next());
                assertEquals("one", firstResult.getString("name"));
                assertFalse(firstResult.next());
            }

            try (NamedPreparedStatement second = query.prepare(connection);
                 ResultSet secondResult = second.executeQuery()) {
                assertTrue(secondResult.next());
                assertEquals("one", secondResult.getString("name"));
                assertFalse(secondResult.next());
            }
        }
    }
}
