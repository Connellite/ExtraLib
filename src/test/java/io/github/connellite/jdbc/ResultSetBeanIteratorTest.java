package io.github.connellite.jdbc;

import io.github.connellite.jdbc.annotation.Column;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResultSetBeanIteratorTest {

    @Test
    void getAllAndGetFirstForBeanClassAndResultSetWork() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (Statement s = c.createStatement()) {
                s.execute("CREATE TABLE one_row (name TEXT, active_flag TEXT)");
                s.execute("INSERT INTO one_row (name, active_flag) VALUES ('row-1', 'true')");
                s.execute("INSERT INTO one_row (name, active_flag) VALUES ('row-2', 'false')");
            }

            List<RowBean> all = ResultSetBeanIterator.getAll(RowBean.class, c, "SELECT name, active_flag FROM one_row ORDER BY name");
            assertEquals(2, all.size());
            assertEquals("row-1", all.get(0).name);
            assertTrue(all.get(0).active);
            assertEquals("row-2", all.get(1).name);
            assertFalse(all.get(1).active);

            Optional<RowBean> first = ResultSetBeanIterator.getFirst(RowBean.class, c, "SELECT name, active_flag FROM one_row ORDER BY name");
            assertTrue(first.isPresent());
            assertEquals("row-1", first.get().name);

            Optional<RowBean> empty = ResultSetBeanIterator.getFirst(RowBean.class, c, "SELECT name, active_flag FROM one_row WHERE 1=0");
            assertTrue(empty.isEmpty());

            try (Statement s = c.createStatement();
                 ResultSet rs1 = s.executeQuery("SELECT name, active_flag FROM one_row ORDER BY name")) {
                List<RowBean> allByResultSet = ResultSetBeanIterator.getAll(rs1, RowBean.class);
                assertEquals(2, allByResultSet.size());
                assertEquals("row-1", allByResultSet.get(0).name);
            }

            try (Statement s = c.createStatement();
                 ResultSet rs2 = s.executeQuery("SELECT name, active_flag FROM one_row ORDER BY name")) {
                Optional<RowBean> firstByResultSet = ResultSetBeanIterator.getFirst(rs2, RowBean.class);
                assertTrue(firstByResultSet.isPresent());
                assertEquals("row-1", firstByResultSet.get().name);
            }
        }
    }

    @Test
    void getAllAndGetFirstSupportSqlParameters() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (Statement s = c.createStatement()) {
                s.execute("CREATE TABLE bean_params_demo (name TEXT, active_flag TEXT)");
                s.execute("INSERT INTO bean_params_demo (name, active_flag) VALUES ('row-1', 'true')");
                s.execute("INSERT INTO bean_params_demo (name, active_flag) VALUES ('row-2', 'false')");
            }

            List<RowBean> all = ResultSetBeanIterator.getAll(
                    RowBean.class,
                    c,
                    "SELECT name, active_flag FROM bean_params_demo WHERE active_flag = ? ORDER BY name",
                    "false");
            assertEquals(1, all.size());
            assertEquals("row-2", all.get(0).name);
            assertFalse(all.get(0).active);

            Optional<RowBean> first = ResultSetBeanIterator.getFirst(
                    RowBean.class,
                    c,
                    "SELECT name, active_flag FROM bean_params_demo WHERE name = ?",
                    "row-2");
            assertTrue(first.isPresent());
            assertEquals("row-2", first.get().name);
            assertFalse(first.get().active);
        }
    }

    @Test
    void scalarGetAllAndGetFirstWorkWithoutBeanClass() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);

            List<Integer> ids = ResultSetBeanIterator.getAll(Integer.class, c, "SELECT id FROM demo ORDER BY id");
            assertEquals(List.of(1, 2), ids);
            assertEquals(1, ResultSetBeanIterator.getFirst(Integer.class, c, "SELECT id FROM demo ORDER BY id").orElseThrow());

            UUID u1 = UUID.randomUUID();
            UUID u2 = UUID.randomUUID();
            try (Statement st = c.createStatement()) {
                st.execute("CREATE TABLE uuid_demo (id TEXT)");
                st.execute("INSERT INTO uuid_demo (id) VALUES ('" + u1 + "')");
                st.execute("INSERT INTO uuid_demo (id) VALUES ('" + u2 + "')");
            }

            List<UUID> uuids = ResultSetBeanIterator.getAll(UUID.class, c, "SELECT id FROM uuid_demo ORDER BY id");
            assertEquals(2, uuids.size());
            assertTrue(uuids.contains(u1));
            assertTrue(uuids.contains(u2));

            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id FROM demo ORDER BY id")) {
                assertEquals(List.of(1, 2), ResultSetBeanIterator.getAll(rs, Integer.class));
            }
        }
    }

    @Test
    void getAllGetFirstAndIteratorFromNamedPreparedStatement() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (Statement s = c.createStatement()) {
                s.execute("CREATE TABLE bean_nps (name TEXT, active_flag TEXT)");
                s.execute("INSERT INTO bean_nps (name, active_flag) VALUES ('row-1', 'true')");
                s.execute("INSERT INTO bean_nps (name, active_flag) VALUES ('row-2', 'false')");
            }
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT name, active_flag FROM bean_nps WHERE active_flag = :flag ORDER BY name")) {
                nps.setString("flag", "true");
                List<RowBean> all = ResultSetBeanIterator.getAll(RowBean.class, nps);
                assertEquals(1, all.size());
                assertEquals("row-1", all.get(0).name);
                assertTrue(all.get(0).active);
            }
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT name, active_flag FROM bean_nps WHERE name = :n ORDER BY name")) {
                nps.setString("n", "row-2");
                Optional<RowBean> first = ResultSetBeanIterator.getFirst(RowBean.class, nps);
                assertTrue(first.isPresent());
                assertEquals("row-2", first.get().name);
                assertFalse(first.get().active);
            }
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT name, active_flag FROM bean_nps WHERE 1 = :zero")) {
                nps.setInt("zero", 0);
                assertTrue(ResultSetBeanIterator.getFirst(RowBean.class, nps).isEmpty());
            }
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c,
                    "SELECT name, active_flag FROM bean_nps WHERE active_flag = :flag ORDER BY name")) {
                nps.setString("flag", "false");
                try (ResultSetBeanIterator<RowBean> it = new ResultSetBeanIterator<>(RowBean.class, nps)) {
                    assertTrue(it.hasNext());
                    RowBean row = it.next();
                    assertEquals("row-2", row.name);
                    assertFalse(row.active);
                    assertFalse(it.hasNext());
                }
            }
        }
    }

    @Test
    void getAllGetFirstWithMapperFromNamedPreparedStatement() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (Statement s = c.createStatement()) {
                s.execute("CREATE TABLE bean_nps_mapper (name TEXT, active_flag TEXT)");
                s.execute("INSERT INTO bean_nps_mapper (name, active_flag) VALUES ('only-false', 'false')");
            }
            SimpleResultSetBeanMapper<RowBean> mapper = new SimpleResultSetBeanMapper<>(RowBean.class);
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c, "SELECT name, active_flag FROM bean_nps_mapper WHERE active_flag = :flag")) {
                nps.setString("flag", "false");
                List<RowBean> all = ResultSetBeanIterator.getAll(mapper, nps);
                assertEquals(1, all.size());
                assertEquals("only-false", all.get(0).name);
                assertFalse(all.get(0).active);
            }
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    c, "SELECT name, active_flag FROM bean_nps_mapper WHERE name = :nm")) {
                nps.setString("nm", "only-false");
                Optional<RowBean> first = ResultSetBeanIterator.getFirst(mapper, nps);
                assertTrue(first.isPresent());
                assertEquals("only-false", first.get().name);
            }
        }
    }

    static class RowBean {
        String name;
        @Column("active_flag")
        Boolean active;
    }
}
