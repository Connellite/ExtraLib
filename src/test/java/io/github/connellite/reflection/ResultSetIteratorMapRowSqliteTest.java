package io.github.connellite.reflection;

import io.github.connellite.jdbc.ResultSetIterator;
import io.github.connellite.jdbc.SqliteMemory;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link io.github.connellite.jdbc.ResultSetIterator} yields {@code Map} rows;
 * {@link SimpleMapBeanMapper#mapRow(Map)} maps those to typed values.
 */
class ResultSetIteratorMapRowSqliteTest {

    @Test
    void iteratorRowsMapToPojoViaMapper() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            SimpleMapBeanMapper<DemoPojo> mapper = new SimpleMapBeanMapper<>(DemoPojo.class);
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
            SimpleMapBeanMapper<DemoRecord> mapper = new SimpleMapBeanMapper<>(DemoRecord.class);
            try (ResultSetIterator it = new ResultSetIterator(c, "SELECT id, name FROM demo ORDER BY id")) {
                assertTrue(it.hasNext());
                DemoRecord first = mapper.mapRow(it.next());
                assertEquals(1, first.id());
                assertEquals("one", first.name());
            }
        }
    }

    @Test
    void mapRowScalarTypeCannotBeInstantiated() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            SimpleMapBeanMapper<Integer> mapper = new SimpleMapBeanMapper<>(Integer.class);
            try (ResultSetIterator it = new ResultSetIterator(c, "SELECT id FROM demo WHERE id = ?", 2)) {
                assertTrue(it.hasNext());
                Map<String, Object> row = it.next();
                IllegalStateException ex = assertThrows(IllegalStateException.class, () -> mapper.mapRow(row));
                assertTrue(ex.getMessage().contains("Cannot instantiate java.lang.Integer"), ex.getMessage());
            }
        }
    }

    @Test
    void mapRowNullMapThrows() throws Exception {
        SimpleMapBeanMapper<DemoPojo> mapper = new SimpleMapBeanMapper<>(DemoPojo.class);
        assertThrows(NullPointerException.class, () -> mapper.mapRow(null));
    }

    @Data
    static class DemoPojo {
        private int id;
        private String name;
    }

    record DemoRecord(int id, String name) {
    }
}
