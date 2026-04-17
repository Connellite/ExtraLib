package io.github.connellite.jdbc;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Wrapper;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultSetWrapperTest {

    @Test
    void unwrap_nullIface_throwsSQLException() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            Statement st = c.createStatement();
            ResultSet delegate = st.executeQuery("SELECT 1");
            try (ResultSetWrapper wrapper = new ResultSetWrapper(st, delegate)) {
                SQLException ex = assertThrows(SQLException.class, () -> wrapper.unwrap(null));
                assertTrue(ex.getMessage().contains("iface must not be null"));
            }
        }
    }

    @Test
    void unwrap_resultSet_returnsWrapper() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            Statement st = c.createStatement();
            ResultSet delegate = st.executeQuery("SELECT 1");
            try (ResultSetWrapper wrapper = new ResultSetWrapper(st, delegate)) {
                assertSame(wrapper, wrapper.unwrap(ResultSet.class));
            }
        }
    }

    @Test
    void unwrap_delegatesToUnderlyingResultSet() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            Statement st = c.createStatement();
            ResultSet delegate = st.executeQuery("SELECT 1");
            try (ResultSetWrapper wrapper = new ResultSetWrapper(st, delegate)) {
                assertSame(delegate, wrapper.unwrap(delegate.getClass()));
            }
        }
    }

    @Test
    void isWrapperFor_resultSet_isTrue() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            Statement st = c.createStatement();
            ResultSet delegate = st.executeQuery("SELECT 1");
            try (ResultSetWrapper wrapper = new ResultSetWrapper(st, delegate)) {
                assertTrue(wrapper.isWrapperFor(ResultSet.class));
            }
        }
    }

    @Test
    void isWrapperFor_wrapperInterface_isTrue() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            Statement st = c.createStatement();
            ResultSet delegate = st.executeQuery("SELECT 1");
            try (ResultSetWrapper wrapper = new ResultSetWrapper(st, delegate)) {
                assertTrue(wrapper.isWrapperFor(Wrapper.class));
            }
        }
    }

    @Test
    void isWrapperFor_delegatesWhenNotImplementedByWrapper() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            Statement st = c.createStatement();
            ResultSet delegate = st.executeQuery("SELECT 1");
            try (ResultSetWrapper wrapper = new ResultSetWrapper(st, delegate)) {
                assertTrue(wrapper.isWrapperFor(delegate.getClass()));
            }
        }
    }
}
