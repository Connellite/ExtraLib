package io.github.connellite.jdbc;

import io.github.connellite.jdbc.annotation.Column;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeanIteratorSimpleResultSetBeanMapperSqliteTest {

    @Test
    void beanIteratorMapsPojoWithAliasesAndTypes() throws Exception {
        UUID id = UUID.randomUUID();
        try (Connection c = SqliteMemory.open()) {
            try (Statement s = c.createStatement()) {
                s.execute("CREATE TABLE bean_demo (" +
                        "id TEXT, " +
                        "name TEXT, " +
                        "active_num INTEGER, " +
                        "amount_text TEXT, " +
                        "born TEXT, " +
                        "created_at TEXT, " +
                        "optional_int INTEGER, " +
                        "primitive_int INTEGER)");
                s.execute("INSERT INTO bean_demo (id, name, active_num, amount_text, born, created_at, optional_int) VALUES (" +
                        "'" + id + "', " +
                        "'alpha', " +
                        "1, " +
                        "'12.50', " +
                        "'2024-07-10', " +
                        "'2024-07-10 09:30:45', " +
                        "NULL)");
                s.execute("INSERT INTO bean_demo (id, name, active_num, amount_text, born, created_at, optional_int, primitive_int) VALUES (" +
                        "'" + UUID.randomUUID() + "', " +
                        "'beta', " +
                        "0, " +
                        "'1.00', " +
                        "'2024-07-11', " +
                        "'2024-07-11 10:00:00', " +
                        "42, " +
                        "7)");
            }

            try (ResultSetBeanIterator<PojoRow> it = new ResultSetBeanIterator<>(c, """
                    SELECT
                        id,
                        name,
                        active_num AS active_flag,
                        amount_text AS amount_value,
                        born,
                        created_at,
                        optional_int,
                        primitive_int
                    FROM bean_demo
                    ORDER BY name
                    """, PojoRow.class)) {

                List<PojoRow> rows = new ArrayList<>();
                for (PojoRow row : it.asIterable()) {
                    rows.add(row);
                }

                assertEquals(2, rows.size());

                PojoRow first = rows.get(0);
                assertEquals(id, first.id);
                assertEquals("alpha", first.name);
                assertTrue(first.active);
                assertEquals(new BigDecimal("12.50"), first.amount);
                assertEquals(LocalDate.of(2024, 7, 10), first.born);
                assertEquals(LocalDateTime.of(2024, 7, 10, 9, 30, 45), first.createdAt);
                assertNull(first.optionalInt);
                assertEquals(0, first.primitiveInt);

                PojoRow second = rows.get(1);
                assertFalse(second.active);
                assertEquals(Integer.valueOf(42), second.optionalInt);
                assertEquals(7, second.primitiveInt);
            }
        }
    }

    @Test
    void simpleBeanMapperCanMapSingleCurrentRow() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (Statement s = c.createStatement()) {
                s.execute("CREATE TABLE one_row (name TEXT, active_flag TEXT)");
                s.execute("INSERT INTO one_row (name, active_flag) VALUES ('row-1', 'true')");
            }
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT name, active_flag FROM one_row")) {
                assertTrue(rs.next());
                SimpleResultSetBeanMapper<PojoMinimal> mapper = new SimpleResultSetBeanMapper<>(PojoMinimal.class);
                PojoMinimal row = mapper.mapRow(rs);
                assertEquals("row-1", row.name);
                assertTrue(row.active);
            }
        }
    }

    @Test
    void beanIteratorCanUseExistingResultSet() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (Statement s = c.createStatement()) {
                s.execute("CREATE TABLE one_row (name TEXT, active_flag TEXT)");
                s.execute("INSERT INTO one_row (name, active_flag) VALUES ('row-1', 'true')");
            }
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT name, active_flag FROM one_row");
                 ResultSetBeanIterator<PojoMinimal> it = new ResultSetBeanIterator<>(rs, PojoMinimal.class)) {
                assertTrue(it.hasNext());
                PojoMinimal row = it.next();
                assertEquals("row-1", row.name);
                assertTrue(row.active);
            }
        }
    }

    @Test
    void staticFindAllAndFindFirstWork() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (Statement s = c.createStatement()) {
                s.execute("CREATE TABLE one_row (name TEXT, active_flag TEXT)");
                s.execute("INSERT INTO one_row (name, active_flag) VALUES ('row-1', 'true')");
                s.execute("INSERT INTO one_row (name, active_flag) VALUES ('row-2', 'false')");
            }

            List<PojoMinimal> all = ResultSetBeanIterator.findAll(c, "SELECT name, active_flag FROM one_row ORDER BY name", PojoMinimal.class);
            assertEquals(2, all.size());
            assertEquals("row-1", all.get(0).name);
            assertTrue(all.get(0).active);
            assertEquals("row-2", all.get(1).name);
            assertFalse(all.get(1).active);

            Optional<PojoMinimal> first = ResultSetBeanIterator.findFirst(c, "SELECT name, active_flag FROM one_row ORDER BY name", PojoMinimal.class);
            assertTrue(first.isPresent());
            assertEquals("row-1", first.get().name);

            Optional<PojoMinimal> empty = ResultSetBeanIterator.findFirst(c, "SELECT name, active_flag FROM one_row WHERE 1=0", PojoMinimal.class);
            assertTrue(empty.isEmpty());

            try (Statement s = c.createStatement();
                 ResultSet rs1 = s.executeQuery("SELECT name, active_flag FROM one_row ORDER BY name")) {
                List<PojoMinimal> allByResultSet = ResultSetBeanIterator.findAll(rs1, PojoMinimal.class);
                assertEquals(2, allByResultSet.size());
                assertEquals("row-1", allByResultSet.get(0).name);
            }

            try (Statement s = c.createStatement();
                 ResultSet rs2 = s.executeQuery("SELECT name, active_flag FROM one_row ORDER BY name")) {
                Optional<PojoMinimal> firstByResultSet = ResultSetBeanIterator.findFirst(rs2, PojoMinimal.class);
                assertTrue(firstByResultSet.isPresent());
                assertEquals("row-1", firstByResultSet.get().name);
            }
        }
    }

    @Test
    void simpleBeanMapperSupportsCustomTypeConverterForPojo() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (Statement s = c.createStatement()) {
                s.execute("CREATE TABLE one_row (name TEXT, active_flag TEXT)");
                s.execute("INSERT INTO one_row (name, active_flag) VALUES ('row-1', 'true')");
            }
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT name, active_flag FROM one_row")) {
                assertTrue(rs.next());
                SimpleResultSetBeanMapper<PojoMinimal> mapper = new SimpleResultSetBeanMapper<>(PojoMinimal.class)
                        .withConverter(Boolean.class, raw -> {
                            if (raw == null) {
                                return null;
                            }
                            return "true".equalsIgnoreCase(raw.toString());
                        });
                PojoMinimal row = mapper.mapRow(rs);
                assertEquals("row-1", row.name);
                assertTrue(row.active);
            }
        }
    }

    @Test
    void missingMappedColumnFailsAtIteratorConstruction() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (Statement s = c.createStatement()) {
                s.execute("CREATE TABLE short_row (id INTEGER)");
                s.execute("INSERT INTO short_row (id) VALUES (1)");
            }

            SQLException ex = assertThrows(SQLException.class,
                    () -> new ResultSetBeanIterator<>(c, "SELECT id FROM short_row", PojoRow.class));
            assertTrue(ex.getMessage().contains("Result set has no column label"));
        }
    }

    @Test
    void extraColumnsInResultSetAreIgnoredForPojo() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (Statement s = c.createStatement()) {
                s.execute("CREATE TABLE one_row (name TEXT, active_flag TEXT, extra_col TEXT)");
                s.execute("INSERT INTO one_row (name, active_flag, extra_col) VALUES ('row-1', 'true', 'ignored')");
            }
            try (ResultSetBeanIterator<PojoMinimal> it = new ResultSetBeanIterator<>(
                    c,
                    "SELECT name, active_flag, extra_col FROM one_row",
                    PojoMinimal.class)) {
                assertTrue(it.hasNext());
                PojoMinimal row = it.next();
                assertEquals("row-1", row.name);
                assertTrue(row.active);
            }
        }
    }

    @Test
    void staticFinalFieldsInPojoAreIgnored() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (Statement s = c.createStatement()) {
                s.execute("CREATE TABLE one_row (name TEXT, active_flag TEXT)");
                s.execute("INSERT INTO one_row (name, active_flag) VALUES ('row-1', 'true')");
            }
            try (ResultSetBeanIterator<PojoWithStaticFinal> it = new ResultSetBeanIterator<>(
                    c,
                    "SELECT name, active_flag FROM one_row",
                    PojoWithStaticFinal.class)) {
                assertTrue(it.hasNext());
                PojoWithStaticFinal row = it.next();
                assertEquals("row-1", row.name);
                assertTrue(row.active);
                assertEquals("CONST", PojoWithStaticFinal.CONST);
            }
        }
    }

    @Test
    void missingMappedColumnFailsAtIteratorConstructionForRecord() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (Statement s = c.createStatement()) {
                s.execute("CREATE TABLE rec_short (id TEXT)");
                s.execute("INSERT INTO rec_short (id) VALUES ('r1')");
            }

            SQLException ex = assertThrows(SQLException.class,
                    () -> new ResultSetBeanIterator<>(c, "SELECT id FROM rec_short", RecordRow3.class));
            assertTrue(ex.getMessage().contains("Result set has no column label"));
        }
    }

    @Test
    void extraColumnsInResultSetAreIgnoredForRecord() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (Statement s = c.createStatement()) {
                s.execute("CREATE TABLE rec_extra (id TEXT, active_flag INTEGER, amount_value TEXT, extra_col TEXT)");
                s.execute("INSERT INTO rec_extra (id, active_flag, amount_value, extra_col) VALUES ('r1', 1, '10.25', 'ignored')");
            }
            try (ResultSetBeanIterator<RecordRow3> it = new ResultSetBeanIterator<>(
                    c,
                    "SELECT id, active_flag, amount_value, extra_col FROM rec_extra",
                    RecordRow3.class)) {
                assertTrue(it.hasNext());
                RecordRow3 row = it.next();
                assertEquals("r1", row.id());
                assertTrue(row.active());
                assertEquals(new BigDecimal("10.25"), row.amount());
            }
        }
    }

    @Test
    void iteratorNextWithoutRowsThrowsNoSuchElement() throws Exception {
        try (Connection c = SqliteMemory.open();
             ResultSetBeanIterator<PojoMinimal> it = new ResultSetBeanIterator<>(c, "SELECT 'x' AS name, 1 AS active_flag WHERE 1=0", PojoMinimal.class)) {
            assertFalse(it.hasNext());
            assertThrows(NoSuchElementException.class, it::next);
        }
    }

    @Test
    void beanIteratorMapsRecordWithSingleAnnotatedComponent() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (ResultSetBeanIterator<RecordRow> it = new ResultSetBeanIterator<>(c, "SELECT 'rec' AS name", RecordRow.class)) {
                assertTrue(it.hasNext());
                RecordRow row = it.next();
                assertEquals("rec", row.name());
            }
        }
    }

    @Test
    void beanIteratorMapsRecordWithMultipleComponents() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (Statement s = c.createStatement()) {
                s.execute("CREATE TABLE rec_demo (id TEXT, active_flag INTEGER, amount_value TEXT)");
                s.execute("INSERT INTO rec_demo (id, active_flag, amount_value) VALUES ('r1', 1, '10.25')");
            }
            try (ResultSetBeanIterator<RecordRow3> it = new ResultSetBeanIterator<>(c,
                    "SELECT id, active_flag, amount_value FROM rec_demo", RecordRow3.class)) {
                assertTrue(it.hasNext());
                RecordRow3 row = it.next();
                assertEquals("r1", row.id());
                assertTrue(row.active());
                assertEquals(new BigDecimal("10.25"), row.amount());
            }
        }
    }

    @Test
    void recordWithAdditionalConstructorsUsesCanonicalForMapping() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (Statement s = c.createStatement()) {
                s.execute("CREATE TABLE rec_ctor_demo (id TEXT, active_flag INTEGER)");
                s.execute("INSERT INTO rec_ctor_demo (id, active_flag) VALUES ('r1', 1)");
            }
            try (ResultSetBeanIterator<RecordWithExtraCtor> it = new ResultSetBeanIterator<>(
                    c,
                    "SELECT id, active_flag FROM rec_ctor_demo",
                    RecordWithExtraCtor.class)) {
                assertTrue(it.hasNext());
                RecordWithExtraCtor row = it.next();
                assertEquals("r1", row.id());
                assertTrue(row.active());
            }
        }
    }

    @Test
    void beanIteratorMapsRecordWithCustomConverter() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (Statement s = c.createStatement()) {
                s.execute("CREATE TABLE rec_time_demo (name TEXT, time_text TEXT)");
                s.execute("INSERT INTO rec_time_demo (name, time_text) VALUES ('r2', '10:15:30')");
            }
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT name, time_text FROM rec_time_demo")) {
                assertTrue(rs.next());
                SimpleResultSetBeanMapper<RecordTimeRow> mapper = new SimpleResultSetBeanMapper<>(RecordTimeRow.class)
                        .withConverter(LocalTime.class, raw -> LocalTime.parse(raw.toString()));
                RecordTimeRow row = mapper.mapRow(rs);
                assertEquals("r2", row.name());
                assertEquals(LocalTime.of(10, 15, 30), row.localTime());
            }
        }
    }

    static class PojoRow {
        @Column("id")
        UUID id;
        String name;
        @Column("active_flag")
        Boolean active;
        @Column("amount_value")
        BigDecimal amount;
        LocalDate born;
        @Column("created_at")
        LocalDateTime createdAt;
        @Column("optional_int")
        Integer optionalInt;
        @Column("primitive_int")
        int primitiveInt;
    }

    static class PojoMinimal {
        String name;
        @Column("active_flag")
        Boolean active;
    }

    static class PojoWithStaticFinal {
        static final String CONST = "CONST";
        String name;
        @Column("active_flag")
        Boolean active;
    }

    record RecordRow(String name) {
    }

    record RecordRow3(String id, @Column("active_flag") boolean active, @Column("amount_value") BigDecimal amount) {
    }

    record RecordTimeRow(String name, @Column("time_text") LocalTime localTime) {
    }

    record RecordWithExtraCtor(String id, @Column("active_flag") boolean active) {
        RecordWithExtraCtor(String id) {
            this(id, false);
        }
    }
}
