package io.github.connellite.jdbc;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultSetStringStreamSqliteTest {

    @Test
    void streamFormatsNumbersAndStrings() throws Exception {
        Connection c = SqliteMemory.open();
        SqliteMemory.bootstrapDemoSchema(c);
        try (Stream<Map<String, String>> stream = ResultSetStringStream.stream(c, "SELECT id, name FROM demo ORDER BY id")) {
            List<Map<String, String>> rows = stream.toList();
            assertEquals(2, rows.size());
            assertEquals("1", rows.get(0).get("id"));
            assertEquals("one", rows.get(0).get("name"));
            assertEquals("2", rows.get(1).get("id"));
            assertEquals("two", rows.get(1).get("name"));
        }
    }

    @Test
    void iteratorStripsTrailingZerosOnDecimalLiteral() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (ResultSetStringIterator it = new ResultSetStringIterator(c, "SELECT 1.500 AS x")) {
                assertTrue(it.hasNext());
                Map<String, String> row = it.next();
                assertEquals("1.5", row.get("x"));
            }
        }
    }

    @Test
    void iteratorNullColumn() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (ResultSetStringIterator it = new ResultSetStringIterator(c, "SELECT CAST(NULL AS INTEGER) AS n")) {
                assertTrue(it.hasNext());
                Map<String, String> row = it.next();
                assertNull(row.get("n"));
            }
        }
    }

    @Test
    void integerOnePointZeroBecomesPlainOne() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (ResultSetStringIterator it = new ResultSetStringIterator(c, "SELECT 1.0 AS x")) {
                assertTrue(it.hasNext());
                assertEquals("1", it.next().get("x"));
            }
        }
    }

    @Test
    void invalidSqlThrows() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            assertThrows(SQLException.class, () -> ResultSetStringStream.stream(c, "SELECT FROM oops"));
        }
    }
}
