package io.github.connellite.jdbc;

import lombok.Data;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ResultSetIterator} yields {@code Map} rows; {@link SimpleResultSetBeanMapper#mapRow(Map)} maps those to typed values.
 */
class ResultSetIteratorMapRowSqliteTest {

    @Test
    void iteratorRowsMapToPojoViaMapper() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            SimpleResultSetBeanMapper<DemoPojo> mapper = new SimpleResultSetBeanMapper<>(DemoPojo.class);
            try (ResultSetIterator it = new ResultSetIterator(c, "SELECT id, name FROM demo ORDER BY id")) {
                List<DemoPojo> out = new ArrayList<>();
                while (it.hasNext()) {
                    out.add(mapper.mapRow(it.next()));
                }
                assertEquals(2, out.size());
                assertEquals(1, out.get(0).getId());
                assertEquals("one", out.get(0).getName());
                assertEquals(2, out.get(1).getId());
                assertEquals("two", out.get(1).getName());
            }
        }
    }

    @Test
    void iteratorRowsMapToRecordViaMapper() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            SimpleResultSetBeanMapper<DemoRecord> mapper = new SimpleResultSetBeanMapper<>(DemoRecord.class);
            try (ResultSetIterator it = new ResultSetIterator(c, "SELECT id, name FROM demo ORDER BY id")) {
                assertTrue(it.hasNext());
                DemoRecord first = mapper.mapRow(it.next());
                assertEquals(1, first.id());
                assertEquals("one", first.name());
            }
        }
    }

    @Test
    void mapRowMapDoesNotSupportScalarMapper() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            SimpleResultSetBeanMapper<Integer> mapper = new SimpleResultSetBeanMapper<>(Integer.class);
            try (ResultSetIterator it = new ResultSetIterator(c, "SELECT id FROM demo WHERE id = ?", 2)) {
                assertTrue(it.hasNext());
                Map<String, Object> row = it.next();
                SQLException ex = assertThrows(SQLException.class, () -> mapper.mapRow(row));
                assertEquals(
                        "Scalar mapping is not supported for mapRow(Map<String, Object>);",
                        ex.getMessage());
            }
        }
    }

    @Test
    void mapRowNullMapThrows() throws Exception {
        SimpleResultSetBeanMapper<DemoPojo> mapper = new SimpleResultSetBeanMapper<>(DemoPojo.class);
        assertThrows(NullPointerException.class, () -> mapper.mapRow((Map<String, Object>) null));
    }

    @Data
    static class DemoPojo {
        private int id;
        private String name;
    }

    record DemoRecord(int id, String name) {
    }
}
