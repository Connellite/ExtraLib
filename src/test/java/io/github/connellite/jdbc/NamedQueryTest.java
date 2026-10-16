package io.github.connellite.jdbc;

import io.github.connellite.jdbc.annotation.Bind;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
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
    void setAllBindsCollectionParameters() {
        NamedQuery.ResolvedQuery resolved = NamedQuery.of("SELECT name FROM demo WHERE id IN (:ids) AND name = :name")
                .setAll(Map.of("ids", List.of(1, 2), "name", "one"))
                .resolve();

        assertEquals("SELECT name FROM demo WHERE id IN (?,?) AND name = ?", resolved.sql());
        assertEquals(List.of(1, 2, "one"), resolved.values());
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
    void clearBindingsRemovesAllBindings() {
        NamedQuery query = NamedQuery.of("SELECT :a + :b AS s")
                .setInt("a", 1)
                .setInt("b", 2)
                .clearBindings();

        assertThrows(IllegalStateException.class, query::resolve);
    }

    @Test
    void clearBindingRemovesSingleBinding() {
        NamedQuery query = NamedQuery.of("SELECT :a + :b AS s")
                .setInt("a", 1)
                .setInt("b", 2)
                .clearBinding("a");

        assertThrows(IllegalStateException.class, query::resolve);
    }

    @Test
    void clearBindingAllowsRebind() {
        NamedQuery.ResolvedQuery resolved = NamedQuery.of("SELECT :x AS v")
                .setInt("x", 1)
                .clearBinding("x")
                .setInt("x", 2)
                .resolve();

        assertEquals("SELECT ? AS v", resolved.sql());
        assertEquals(List.of(2), resolved.values());
    }

    @Test
    void clearBindingOnUnboundParameterIsNoOp() {
        NamedQuery.ResolvedQuery resolved = NamedQuery.of("SELECT :x AS v")
                .setInt("x", 1)
                .clearBinding("missing")
                .resolve();

        assertEquals("SELECT ? AS v", resolved.sql());
        assertEquals(List.of(1), resolved.values());
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

    @Test
    void resolveKeepsOutPlaceholderWithoutAnInValue() {
        NamedQuery.ResolvedQuery resolved = NamedQuery.of("{:ret = call add(:a, :b)}")
                .setInt("a", 2)
                .setInt("b", 3)
                .registerOutParameter("ret", Types.INTEGER)
                .resolve();

        assertEquals("{? = call add(?, ?)}", resolved.sql());
        assertEquals(Arrays.asList(null, 2, 3), resolved.values());
    }

    @Test
    void prepareRejectsOutRegistration() throws Exception {
        NamedQuery query = NamedQuery.of("{:ret = call add(:a)}")
                .setInt("a", 1)
                .registerOutParameter("ret", Types.INTEGER);
        try (Connection connection = SqliteMemory.open()) {
            assertThrows(IllegalStateException.class, () -> query.prepare(connection));
        }
    }

    @Test
    void setBeanBindsPrivateFieldsAndNull() {
        NamedQuery.ResolvedQuery resolved = NamedQuery.of("SELECT :id AS id, :name AS name")
                .setBean(new NullableRow(1, null))
                .resolve();

        assertEquals("SELECT ? AS id, ? AS name", resolved.sql());
        assertEquals(Arrays.asList(1, null), resolved.values());
    }

    @Test
    void setBeanRenamesRecordComponent() {
        NamedQuery.ResolvedQuery resolved = NamedQuery.of("SELECT :user_name AS n")
                .setBean(new Renamed("ada"))
                .resolve();

        assertEquals("SELECT ? AS n", resolved.sql());
        assertEquals(List.of("ada"), resolved.values());
    }

    @Test
    void setBeanRejectsTypeWithoutBind() {
        assertThrows(IllegalArgumentException.class, () ->
                NamedQuery.of("SELECT :name AS n").setBean(new Plain("a")));
    }

    @Test
    void setBeanRejectsNullBean() {
        assertThrows(NullPointerException.class, () ->
                NamedQuery.of("SELECT :id AS id").setBean(null));
    }

    @Test
    void setBeanRejectsWhenNoBindNameMatchesSql() {
        assertThrows(IllegalArgumentException.class, () ->
                NamedQuery.of("SELECT :name AS n").setBean(new OnlyId(1)));
    }

    @Test
    void setBeanSkipsBindNamesAbsentFromSql() {
        NamedQuery.ResolvedQuery resolved = NamedQuery.of("SELECT :id AS id")
                .setBean(new Extra(7, "skip"))
                .resolve();

        assertEquals("SELECT ? AS id", resolved.sql());
        assertEquals(List.of(7), resolved.values());
    }

    @Test
    void setBeanLeavesUnmatchedSqlParameterUnbound() {
        assertThrows(IllegalStateException.class, () ->
                NamedQuery.of("SELECT :id AS id, :name AS name")
                        .setBean(new OnlyId(1))
                        .resolve());
    }

    @Test
    void setBeanAppliesConverterBeforeNullCheck() {
        NamedQuery.ResolvedQuery resolved = NamedQuery.of("SELECT :body AS b, :note AS n")
                .setBean(new Converted("payload", null))
                .resolve();

        assertEquals("SELECT ? AS b, ? AS n", resolved.sql());
        assertEquals(List.of("json:payload", ""), resolved.values());
    }

    @Test
    void setBeanWrapsConverterFailure() {
        IllegalStateException failure = assertThrows(IllegalStateException.class, () ->
                NamedQuery.of("SELECT :name AS n").setBean(new Failing("x")));
        assertEquals("convert failed", failure.getCause().getMessage());
    }

    @Test
    void setBeanRejectsStaticBind() {
        assertThrows(IllegalArgumentException.class, () ->
                NamedQuery.of("SELECT :id AS id").setBean(new StaticBind()));
    }

    @Test
    void setBeanRejectsDuplicateParameterName() {
        assertThrows(IllegalArgumentException.class, () ->
                NamedQuery.of("SELECT :id AS id").setBean(new Duplicate(1, 2)));
    }

    @Test
    void setBeanBindsCollectionParameters() {
        NamedQuery.ResolvedQuery resolved = NamedQuery.of("SELECT name FROM demo WHERE id IN (:ids) AND name = :name")
                .setBean(new IdsAndName(List.of(1, 2), "one"))
                .resolve();

        assertEquals("SELECT name FROM demo WHERE id IN (?,?) AND name = ?", resolved.sql());
        assertEquals(List.of(1, 2, "one"), resolved.values());
    }

    @Test
    void setBeanReadsInheritedFields() {
        NamedQuery.ResolvedQuery resolved = NamedQuery.of("SELECT :id AS id, :name AS name")
                .setBean(new Child(4, "child"))
                .resolve();

        assertEquals(Arrays.asList(4, "child"), resolved.values());
    }

    static final class NullableRow {
        @Bind
        private final Integer id;
        @Bind
        private final String name;

        NullableRow(Integer id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    record Renamed(@Bind("user_name") String name) {
    }

    record Plain(String name) {
    }

    record OnlyId(@Bind int id) {
    }

    record IdsAndName(@Bind List<Integer> ids, @Bind String name) {
    }

    record Extra(@Bind int id, @Bind String unused) {
    }

    record Converted(
            @Bind(converter = PrefixConverter.class) String body,
            @Bind(converter = NullToEmpty.class) String note) {
    }

    record Failing(@Bind(converter = FailingConverter.class) String name) {
    }

    static final class StaticBind {
        @Bind
        static int id;
    }

    record Duplicate(@Bind("id") int a, @Bind("id") int b) {
    }

    static class Parent {
        @Bind
        private final int id;

        Parent(int id) {
            this.id = id;
        }
    }

    static final class Child extends Parent {
        @Bind
        private final String name;

        Child(int id, String name) {
            super(id);
            this.name = name;
        }
    }

    static final class PrefixConverter implements TypeConverter<String> {
        @Override
        public String convert(Object raw) {
            return "json:" + raw;
        }
    }

    static final class NullToEmpty implements TypeConverter<String> {
        @Override
        public String convert(Object raw) {
            return raw == null ? "" : raw.toString();
        }
    }

    static final class FailingConverter implements TypeConverter<Object> {
        @Override
        public Object convert(Object raw) throws SQLException {
            throw new SQLException("convert failed");
        }
    }
}
