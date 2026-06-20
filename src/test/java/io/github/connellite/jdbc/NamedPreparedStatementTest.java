package io.github.connellite.jdbc;

import io.github.connellite.jdbc.parser.HashPrefixSqlParser;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NamedPreparedStatementTest {

    @Test
    void replacesNamedParameterWithQuestionMark() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (NamedPreparedStatement nps =
                         new NamedPreparedStatement(c, "SELECT :x AS v")) {
                assertEquals("SELECT ? AS v", nps.getParsedSql());
                assertEquals(List.of("x"), nps.getParameterOrder());
            }
        }
    }

    @Test
    void customHashPrefixParserReplacesNamedParameterWithQuestionMark() throws Exception {
        try (Connection c = connectionThatAcceptsAnyPreparedSql()) {
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT #id AS v, '#not_param' AS literal",
                    new HashPrefixSqlParser())) {
                assertEquals("SELECT ? AS v, '#not_param' AS literal", nps.getParsedSql());
                assertEquals(List.of("id"), nps.getParameterOrder());
            }
        }
    }

    @Test
    void hashPrefixParserIgnoresHashInsideCommentsAndHonorsEscapes() throws Exception {
        try (Connection c = connectionThatAcceptsAnyPreparedSql()) {
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT \\#escaped AS e -- #ignored\n, #id AS v",
                    new HashPrefixSqlParser())) {
                assertEquals("SELECT #escaped AS e -- #ignored\n, ? AS v", nps.getParsedSql());
                assertEquals(List.of("id"), nps.getParameterOrder());
            }
        }
    }

    @Test
    void colonInsideStringLiteralIsNotNamedParameter() throws Exception {
        try (Connection c = connectionThatAcceptsAnyPreparedSql()) {
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT :id AS v, '12:00' AS t, ':prefix' AS u")) {
                assertEquals("SELECT ? AS v, '12:00' AS t, ':prefix' AS u", nps.getParsedSql());
                assertEquals(List.of("id"), nps.getParameterOrder());
            }
        }
    }

    /**
     * PostgreSQL-style cast ({@code ::type}) is not valid SQLite syntax; use a connection stub so
     * {@link NamedPreparedStatement} does not call the real driver's {@code prepareStatement}.
     */
    @Test
    void doubleColonCastIsLiteralNotSecondNamedParameter() throws Exception {
        try (Connection c = connectionThatAcceptsAnyPreparedSql()) {
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT :id::jsonb AS v")) {
                assertEquals("SELECT ?::jsonb AS v", nps.getParsedSql());
                assertEquals(List.of("id"), nps.getParameterOrder());
            }
        }
    }

    @Test
    void namedParameterInsideLineCommentIsIgnored() throws Exception {
        try (Connection c = connectionThatAcceptsAnyPreparedSql()) {
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT 1 AS v -- comment :not_param\n, :id AS id_value")) {
                assertEquals("SELECT 1 AS v -- comment :not_param\n, ? AS id_value", nps.getParsedSql());
                assertEquals(List.of("id"), nps.getParameterOrder());
            }
        }
    }

    @Test
    void namedParameterInsideBlockCommentIsIgnored() throws Exception {
        try (Connection c = connectionThatAcceptsAnyPreparedSql()) {
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT /* :hidden */ :id AS id_value")) {
                assertEquals("SELECT /* :hidden */ ? AS id_value", nps.getParsedSql());
                assertEquals(List.of("id"), nps.getParameterOrder());
            }
        }
    }

    @Test
    void sameNameAppearsTwiceInParameterOrder() throws Exception {
        try (Connection c = connectionThatAcceptsAnyPreparedSql()) {
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT :id + :id AS sum_value")) {
                assertEquals(
                        "SELECT ? + ? AS sum_value",
                        nps.getParsedSql());
                assertEquals(List.of("id", "id"), nps.getParameterOrder());
            }
        }
    }

    @Test
    void positionalOnlySqlPreservesQuestionMarks() throws Exception {
        try (Connection c = connectionThatAcceptsAnyPreparedSql()) {
            try (NamedPreparedStatement nps =
                         new NamedPreparedStatement(c, "SELECT ? AS id_value, ? AS name_value")) {
                assertEquals("SELECT ? AS id_value, ? AS name_value", nps.getParsedSql());
                assertEquals(List.of("?", "?"), nps.getParameterOrder());
            }
        }
    }

    @Test
    void mixingNamedAndPositionalThrows() throws SQLException {
        try (Connection c = SqliteMemory.open()) {
            assertThrows(IllegalArgumentException.class, () ->
                    new NamedPreparedStatement(c, "SELECT 1 WHERE x = :id AND y = ?"));
        }
    }

    @Test
    void executeQuerySelectsRow() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT name FROM demo WHERE id = :id")) {
                nps.setObject("id", 1);
                try (ResultSet rs = nps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals("one", rs.getString("name"));
                    assertFalse(rs.next());
                }
            }
        }
    }

    @Test
    void repeatedNamedParameterSingleBindSetsBothPositions() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT COUNT(*) AS n FROM demo WHERE id = :id OR id = :id")) {
                nps.setObject("id", 2);
                try (ResultSet rs = nps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt("n"));
                }
            }
        }
    }

    @Test
    void setAllBindsFromMap() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT name FROM demo WHERE id = :id AND name = :name")) {
                nps.setAll(Map.of("id", 1, "name", "one"));
                try (ResultSet rs = nps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals("one", rs.getString("name"));
                }
            }
        }
    }

    @Test
    void typedSettersBindValues() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT :i AS i, :l AS l, :s AS s, :b AS b, :n AS n")) {
                nps.setInt("i", 42)
                        .setLong("l", 10_000_000_000L)
                        .setString("s", "value")
                        .setBoolean("b", true)
                        .setNull("n", Types.INTEGER);

                try (ResultSet rs = nps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(42, rs.getInt("i"));
                    assertEquals(10_000_000_000L, rs.getLong("l"));
                    assertEquals("value", rs.getString("s"));
                    assertTrue(rs.getBoolean("b"));
                    assertEquals(0, rs.getInt("n"));
                    assertTrue(rs.wasNull());
                    assertFalse(rs.next());
                }
            }
        }
    }

    @Test
    void executeWithoutBindingAllNamedParametersThrows() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT name FROM demo WHERE id = :id")) {
                assertThrows(IllegalStateException.class, nps::executeQuery);
            }
        }
    }

    @Test
    void unknownNamedParameterThrows() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT name FROM demo WHERE id = :id")) {
                assertThrows(IllegalArgumentException.class, () -> nps.setObject("missing", 1));
            }
        }
    }

    @Test
    void clearParametersClearsBindingStateForExecute() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT name FROM demo WHERE id = :id")) {
                nps.setObject("id", 1);
                nps.clearParameters();
                assertThrows(IllegalStateException.class, nps::executeQuery);
            }
        }
    }

    @Test
    void batchAddsTwoRows() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "INSERT INTO demo (id, name) VALUES (:id, :name)")) {
                nps.setObject("id", 10);
                nps.setObject("name", "ten");
                nps.addBatch();
                nps.setObject("id", 11);
                nps.setObject("name", "eleven");
                nps.addBatch();
                int[] counts = nps.executeBatch();
                assertArrayEquals(new int[]{1, 1}, counts);
            }
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) AS n FROM demo WHERE id IN (10, 11)")) {
                assertTrue(rs.next());
                assertEquals(2, rs.getInt("n"));
            }
        }
    }

    @Test
    void unwrapReturnsUnderlyingPreparedStatement() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (NamedPreparedStatement nps =
                         new NamedPreparedStatement(c, "SELECT 1")) {
                assertTrue(nps.unwrap() instanceof PreparedStatement);
            }
        }
    }

    @Test
    void selectLiteralWithoutBindSucceeds() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (NamedPreparedStatement nps = new NamedPreparedStatement(c, "SELECT 42 AS v")) {
                try (ResultSet rs = nps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(42, rs.getInt("v"));
                    assertFalse(rs.next());
                }
            }
        }
    }

    @Test
    void threeDistinctParameters() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT :a + :b + :c AS s")) {
                nps.setInt("a", 1).setInt("b", 2).setInt("c", 3);
                try (ResultSet rs = nps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(6, rs.getInt("s"));
                }
            }
        }
    }

    @Test
    void fiveDistinctParameters() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT :p1 * :p2 + :p3 - :p4 + :p5 AS s")) {
                nps.setInt("p1", 2)
                        .setInt("p2", 3)
                        .setInt("p3", 10)
                        .setInt("p4", 4)
                        .setInt("p5", 1);
                try (ResultSet rs = nps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(2 * 3 + 10 - 4 + 1, rs.getInt("s"));
                }
            }
        }
    }

    @Test
    void eightDistinctParametersViaSetAll() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            String sql = "SELECT :p1+:p2+:p3+:p4+:p5+:p6+:p7+:p8 AS s";
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("p1", 1);
            params.put("p2", 1);
            params.put("p3", 1);
            params.put("p4", 1);
            params.put("p5", 1);
            params.put("p6", 1);
            params.put("p7", 1);
            params.put("p8", 1);
            try (NamedPreparedStatement nps = new NamedPreparedStatement(c, sql)) {
                nps.setAll(params);
                try (ResultSet rs = nps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(8, rs.getInt("s"));
                }
            }
        }
    }

    @Test
    void twelveDistinctParameters() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            StringBuilder sql = new StringBuilder("SELECT ");
            for (int i = 1; i <= 12; i++) {
                if (i > 1) {
                    sql.append('+');
                }
                sql.append(":p").append(i);
            }
            sql.append(" AS s");
            try (NamedPreparedStatement nps = new NamedPreparedStatement(c, sql.toString())) {
                for (int i = 1; i <= 12; i++) {
                    nps.setInt("p" + i, i);
                }
                try (ResultSet rs = nps.executeQuery()) {
                    assertTrue(rs.next());
                    int expected = 0;
                    for (int i = 1; i <= 12; i++) {
                        expected += i;
                    }
                    assertEquals(expected, rs.getInt("s"));
                }
            }
        }
    }

    @Test
    void parameterOrderMatchesSqlOccurrenceOrder() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT :z AS z, :a AS a, :m AS m")) {
                assertEquals(
                        List.of("z", "a", "m"),
                        nps.getParameterOrder());
                nps.setString("z", "Z").setString("a", "A").setString("m", "M");
                try (ResultSet rs = nps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals("Z", rs.getString("z"));
                    assertEquals("A", rs.getString("a"));
                    assertEquals("M", rs.getString("m"));
                }
            }
        }
    }

    @Test
    void sameNameThreeTimesSingleBind() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT :x + :x + :x AS s")) {
                assertEquals(List.of("x", "x", "x"), nps.getParameterOrder());
                nps.setInt("x", 7);
                try (ResultSet rs = nps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(21, rs.getInt("s"));
                }
            }
        }
    }

    @Test
    void interleavedTwoNamesEachTwice() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT :a * :b + :a * :b AS s")) {
                assertEquals(List.of("a", "b", "a", "b"), nps.getParameterOrder());
                nps.setInt("a", 2).setInt("b", 3);
                try (ResultSet rs = nps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(12, rs.getInt("s"));
                }
            }
        }
    }

    @Test
    void repeatedIdInWhereAgainstDemo() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT name FROM demo WHERE id = :id AND (SELECT COUNT(*) FROM demo WHERE id = :id) = 1")) {
                nps.setInt("id", 1);
                try (ResultSet rs = nps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals("one", rs.getString("name"));
                }
            }
        }
    }

    @Test
    void rebindOverwritesAllOccurrences() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT :v + :v AS s")) {
                nps.setInt("v", 1);
                nps.setInt("v", 5);
                try (ResultSet rs = nps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(10, rs.getInt("s"));
                }
            }
        }
    }

    @Test
    void executeUpdateWithTwoNamedParameters() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "UPDATE demo SET name = :name WHERE id = :id")) {
                nps.setString("name", "updated").setInt("id", 1);
                assertEquals(1, nps.executeUpdate());
            }
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT name FROM demo WHERE id = 1")) {
                assertTrue(rs.next());
                assertEquals("updated", rs.getString("name"));
            }
        }
    }

    @Test
    void executeReturnsTrueForSelectAndResultSetReadable() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT name FROM demo WHERE id = :id")) {
                nps.setInt("id", 2);
                assertTrue(nps.execute());
                PreparedStatement ps = nps.unwrap();
                try (ResultSet rs = ps.getResultSet()) {
                    assertNotNull(rs);
                    assertTrue(rs.next());
                    assertEquals("two", rs.getString("name"));
                }
            }
        }
    }

    @Test
    void batchFiveInsertsSameStatement() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "INSERT INTO demo (id, name) VALUES (:id, :name)")) {
                for (int i = 0; i < 5; i++) {
                    int id = 100 + i;
                    nps.setInt("id", id);
                    nps.setString("name", "n" + id);
                    nps.addBatch();
                }
                int[] counts = nps.executeBatch();
                assertArrayEquals(new int[]{1, 1, 1, 1, 1}, counts);
            }
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT COUNT(*) AS n FROM demo WHERE id BETWEEN 100 AND 104")) {
                assertTrue(rs.next());
                assertEquals(5, rs.getInt("n"));
            }
        }
    }

    @Test
    void partialSetAllStillFailsUntilAllNamesBound() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT :a + :b AS s")) {
                nps.setAll(Map.of("a", 1));
                assertThrows(IllegalStateException.class, nps::executeQuery);
            }
        }
    }

    @Test
    void clearParametersThenRebindAndExecute() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT :v AS v")) {
                nps.setInt("v", 1);
                nps.clearParameters();
                assertThrows(IllegalStateException.class, nps::executeQuery);
                nps.setInt("v", 99);
                try (ResultSet rs = nps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(99, rs.getInt("v"));
                }
            }
        }
    }

    @Test
    void nullConnectionThrows() {
        assertThrows(NullPointerException.class, () ->
                new NamedPreparedStatement(null, "SELECT 1"));
    }

    @Test
    void underscoreAndDigitsInParameterName() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT :_id + :col2 AS s")) {
                nps.setInt("_id", 10).setInt("col2", 3);
                try (ResultSet rs = nps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(13, rs.getInt("s"));
                }
            }
        }
    }

    @Test
    void dollarInParameterName() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT :$v AS v")) {
                nps.setInt("$v", 5);
                try (ResultSet rs = nps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(5, rs.getInt("v"));
                }
            }
        }
    }

    @Test
    void unicodeLettersInParameterName() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT :café AS v")) {
                nps.setInt("café", 77);
                try (ResultSet rs = nps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(77, rs.getInt("v"));
                }
            }
        }
    }

    @Test
    void joinChildTableWithTwoNamedParameters() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT c.tag AS tag FROM demo d "
                            + "JOIN child c ON c.demo_id = d.id "
                            + "WHERE d.id = :demoId AND c.cid = :childId")) {
                nps.setInt("demoId", 1).setInt("childId", 1);
                try (ResultSet rs = nps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals("c", rs.getString("tag"));
                    assertFalse(rs.next());
                }
            }
        }
    }

    @Test
    void setNullThenSelectColumnIsSqlNull() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT :n AS n")) {
                nps.setNull("n", Types.INTEGER);
                try (ResultSet rs = nps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(0, rs.getInt("n"));
                    assertTrue(rs.wasNull());
                }
            }
        }
    }

    @Test
    void setObjectNullBindsSqlNull() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT :x AS x")) {
                nps.setObject("x", null);
                try (ResultSet rs = nps.executeQuery()) {
                    assertTrue(rs.next());
                    assertNull(rs.getString("x"));
                    assertTrue(rs.wasNull());
                }
            }
        }
    }

    /**
     * SQLite may reject PostgreSQL-only syntax at {@code prepareStatement};
     * {@link NamedPreparedStatement} always prepares, so tests that only assert rewritten SQL use this stub.
     */
    private static Connection connectionThatAcceptsAnyPreparedSql() {
        return (Connection) Proxy.newProxyInstance(
                NamedPreparedStatementTest.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("prepareStatement".equals(name) && args != null && args.length >= 1 && args[0] instanceof String) {
                        return preparedStatementStub();
                    }
                    return switch (name) {
                        case "close", "commit", "rollback" -> null;
                        case "isClosed" -> false;
                        case "getAutoCommit" -> true;
                        default -> throw new UnsupportedOperationException(name);
                    };
                });
    }

    private static PreparedStatement preparedStatementStub() {
        return (PreparedStatement) Proxy.newProxyInstance(
                NamedPreparedStatementTest.class.getClassLoader(),
                new Class<?>[]{PreparedStatement.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("close".equals(name)
                            || "clearParameters".equals(name)
                            || name.startsWith("set")
                            || "addBatch".equals(name)) {
                        return null;
                    }
                    return switch (name) {
                        case "unwrap" -> proxy;
                        case "executeQuery", "executeUpdate", "execute" ->
                                throw new SQLException("stub prepared statement");
                        case "executeBatch" -> new int[0];
                        default -> throw new UnsupportedOperationException(name);
                    };
                });
    }
}
