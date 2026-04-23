package io.github.connellite.jdbc;

import io.github.connellite.jdbc.annotation.Column;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultSetBeanStreamSqliteTest {

    @Test
    void streamFromConnectionAndSql() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (Statement s = c.createStatement()) {
                s.execute("CREATE TABLE bean_stream_demo (name TEXT, active_flag TEXT)");
                s.execute("INSERT INTO bean_stream_demo (name, active_flag) VALUES ('row-1', 'true')");
                s.execute("INSERT INTO bean_stream_demo (name, active_flag) VALUES ('row-2', 'false')");
            }

            try (Stream<RowBean> stream = ResultSetBeanStream.stream(
                    RowBean.class,
                    c,
                    "SELECT name, active_flag FROM bean_stream_demo ORDER BY name"
            )) {
                List<RowBean> rows = stream.toList();
                assertEquals(2, rows.size());
                assertEquals("row-1", rows.get(0).name);
                assertTrue(rows.get(0).active);
                assertEquals("row-2", rows.get(1).name);
            }
        }
    }

    @Test
    void streamFromConnectionAndSqlWithParams() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (Statement s = c.createStatement()) {
                s.execute("CREATE TABLE bean_stream_params_demo (name TEXT, active_flag TEXT)");
                s.execute("INSERT INTO bean_stream_params_demo (name, active_flag) VALUES ('row-1', 'true')");
                s.execute("INSERT INTO bean_stream_params_demo (name, active_flag) VALUES ('row-2', 'false')");
                s.execute("INSERT INTO bean_stream_params_demo (name, active_flag) VALUES ('row-3', 'true')");
            }

            try (Stream<RowBean> stream = ResultSetBeanStream.stream(
                    RowBean.class,
                    c,
                    "SELECT name, active_flag FROM bean_stream_params_demo WHERE active_flag = ? ORDER BY name",
                    "true"
            )) {
                List<RowBean> rows = stream.toList();
                assertEquals(2, rows.size());
                assertEquals("row-1", rows.get(0).name);
                assertTrue(rows.get(0).active);
                assertEquals("row-3", rows.get(1).name);
                assertTrue(rows.get(1).active);
            }
        }
    }

    @Test
    void streamFromExistingResultSet() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            try (Statement s = c.createStatement()) {
                s.execute("CREATE TABLE bean_stream_demo (name TEXT, active_flag TEXT)");
                s.execute("INSERT INTO bean_stream_demo (name, active_flag) VALUES ('row-1', 'true')");
            }

            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT name, active_flag FROM bean_stream_demo");
                 Stream<RowBean> stream = ResultSetBeanStream.stream(rs, RowBean.class)) {
                RowBean row = stream.findFirst().orElseThrow();
                assertEquals("row-1", row.name);
                assertTrue(row.active);
            }
        }
    }

    static class RowBean {
        String name;
        @Column("active_flag")
        Boolean active;
    }
}
