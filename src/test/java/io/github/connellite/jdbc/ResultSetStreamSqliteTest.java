package io.github.connellite.jdbc;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultSetStreamSqliteTest {

    @Test
    void streamRowsThenClose() throws Exception {
        Connection c = SqliteMemory.open();
        SqliteMemory.bootstrapDemoSchema(c);
        try (Stream<Map<String, Object>> stream = ResultSetStream.stream(c, "SELECT id, name FROM demo ORDER BY id")) {
            List<Map<String, Object>> rows = stream.toList();
            assertEquals(2, rows.size());
            assertEquals(1, rows.get(0).get("id"));
            assertEquals("one", rows.get(0).get("name"));
        }
    }

    @Test
    void invalidSqlThrows() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            assertThrows(SQLException.class, () -> ResultSetStream.stream(c, "SELECT FROM oops"));
        }
    }

    @Test
    void iteratorSamePackageConstructs() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            try (ResultSetIterator it = new ResultSetIterator(c, "SELECT id FROM demo ORDER BY id")) {
                assertTrue(it.hasNext());
                Map<String, Object> row = it.next();
                assertEquals(1, row.get("id"));
            }
        }
    }

    @Test
    void forEachRowIterable() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            try (ResultSetIterator it = new ResultSetIterator(c, "SELECT id FROM demo ORDER BY id")) {
                List<Integer> ids = new ArrayList<>();
                for (Map<String, Object> row : it.asIterable()) {
                    ids.add(((Number) row.get("id")).intValue());
                }
                assertEquals(List.of(1, 2), ids);
            }
        }
    }

    @Test
    void iteratorUsesColumnAliasAsMapKey() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            try (ResultSetIterator it = new ResultSetIterator(c, "SELECT id AS pk, name AS label FROM demo ORDER BY id")) {
                assertTrue(it.hasNext());
                Map<String, Object> row = it.next();
                assertEquals(1, row.get("pk"));
                assertEquals("one", row.get("label"));
                assertNull(row.get("id"));
            }
        }
    }

    @Test
    void iteratorAndStreamCanUseExistingResultSet() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);

            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id, name FROM demo ORDER BY id");
                 ResultSetIterator it = new ResultSetIterator(rs)) {
                assertTrue(it.hasNext());
                assertEquals(1, it.next().get("id"));
            }

            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id FROM demo ORDER BY id");
                 Stream<Map<String, Object>> stream = ResultSetStream.stream(rs)) {
                assertEquals(List.of(1, 2), stream.map(row -> ((Number) row.get("id")).intValue()).toList());
            }
        }
    }

    @Test
    void iteratorStaticFindMethodsWorkForQueryAndResultSet() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);

            List<Map<String, Object>> allByQuery = ResultSetIterator.getAll(c, "SELECT id FROM demo ORDER BY id");
            assertEquals(2, allByQuery.size());
            assertEquals(1, allByQuery.get(0).get("id"));

            assertEquals(1, ResultSetIterator.getFirst(c, "SELECT id FROM demo ORDER BY id").orElseThrow().get("id"));
            assertTrue(ResultSetIterator.getFirst(c, "SELECT id FROM demo WHERE 1=0").isEmpty());

            try (Statement st = c.createStatement();
                 ResultSet rs1 = st.executeQuery("SELECT id FROM demo ORDER BY id")) {
                List<Map<String, Object>> allByRs = ResultSetIterator.getAll(rs1);
                assertEquals(2, allByRs.size());
                assertEquals(2, allByRs.get(1).get("id"));
            }
            try (Statement st = c.createStatement();
                 ResultSet rs2 = st.executeQuery("SELECT id FROM demo ORDER BY id")) {
                assertEquals(1, ResultSetIterator.getFirst(rs2).orElseThrow().get("id"));
            }
        }
    }
}
