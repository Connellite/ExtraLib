package io.github.connellite.jdbc;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryUtilsSqliteTest {

    @Test
    void selectQueryReturnsDetachedRows() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            try (ResultSet rs = QueryUtils.selectQuery(c, "SELECT name FROM demo WHERE id = ?", 1)) {
                assertTrue(rs.next());
                assertEquals("one", rs.getString("name"));
                assertFalse(rs.next());
            }
        }
    }

    @Test
    void executeQueryUpdatesRow() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            assertFalse(QueryUtils.executeQuery(c, "UPDATE demo SET name = ? WHERE id = ?", "uno", 1));
            try (ResultSet rs = QueryUtils.selectQuery(c, "SELECT name FROM demo WHERE id = ?", 1)) {
                assertTrue(rs.next());
                assertEquals("uno", rs.getString("name"));
            }
        }
    }

    @Test
    void methodsThrowOriginalSQLException() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);

            assertThrows(SQLException.class, () -> QueryUtils.selectQuery(c, "SELECT * FROM missing_table", (Object) null));
            assertThrows(SQLException.class, () -> QueryUtils.executeQuery(c, "UPDATE missing_table SET x = 1", (Object) null));
        }
    }

    @Test
    void executeBatchRunsAllParameterSets() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            int[] counts = QueryUtils.executeBatch(
                    c,
                    "INSERT INTO demo (id, name) VALUES (?, ?)",
                    new Object[][]{{10, "ten"}, {11, "eleven"}});
            assertEquals(2, counts.length);
            try (ResultSet rs = QueryUtils.selectQuery(c, "SELECT COUNT(*) AS n FROM demo WHERE id IN (10, 11)")) {
                assertTrue(rs.next());
                assertEquals(2, rs.getInt("n"));
            }
        }
    }

    @Test
    void executeBatchEmptyReturnsEmptyArray() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            assertArrayEquals(new int[0], QueryUtils.executeBatch(c, "INSERT INTO demo (id, name) VALUES (?, ?)", new Object[0][]));
            assertArrayEquals(new int[0], QueryUtils.executeBatch(c, "INSERT INTO demo (id, name) VALUES (?, ?)", null));
        }
    }
}
