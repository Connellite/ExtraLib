package io.github.connellite.jdbc;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcResultSetStreamSqliteTest {

    @Test
    void streamRowsThenClose() throws Exception {
        Connection c = SqliteMemory.open();
        SqliteMemory.bootstrapDemoSchema(c);
        try (Stream<Map<String, Object>> stream = JdbcResultSetStream.stream(c, "SELECT id, name FROM demo ORDER BY id")) {
            List<Map<String, Object>> rows = stream.collect(Collectors.toList());
            assertEquals(2, rows.size());
            assertEquals("1", rows.get(0).get("id"));
            assertEquals("one", rows.get(0).get("name"));
        }
    }

    @Test
    void invalidSqlThrows() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            assertThrows(SQLException.class, () -> JdbcResultSetStream.stream(c, "SELECT FROM oops"));
        }
    }

    @Test
    void iteratorSamePackageConstructs() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            try (JdbcResultSetIterator it = new JdbcResultSetIterator(c, "SELECT id FROM demo ORDER BY id")) {
                assertTrue(it.hasNext());
                Map<String, Object> row = it.next();
                assertEquals("1", row.get("id"));
            }
        }
    }
}
