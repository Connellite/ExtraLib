package io.github.connellite.jdbc;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryUtilsH2Test {

    @Test
    void executeCallBindsInParameters() throws Exception {
        try (Connection connection = openWithAliases()) {
            QueryUtils.executeCall(connection, "{call INSERT_DEMO(?, ?)}", 10, "ten");
            try (ResultSet resultSet = connection.createStatement().executeQuery(
                    "SELECT name FROM demo WHERE id = 10")) {
                assertTrue(resultSet.next());
                assertEquals("ten", resultSet.getString("name"));
            }
        }
    }

    @Test
    void selectFromCallReturnsRows() throws Exception {
        try (Connection connection = openWithAliases()) {
            try (ResultSet resultSet = QueryUtils.selectFromCall(connection, "{call FIND_DEMO_NAME(?)}", 1)) {
                assertTrue(resultSet.next());
                assertEquals("one", resultSet.getString("name"));
                assertFalse(resultSet.next());
            }
        }
    }

    @Test
    void selectFromCallCachedReturnsRowsAfterStatementClosed() throws Exception {
        ResultSet resultSet;
        try (Connection connection = openWithAliases()) {
            resultSet = QueryUtils.selectFromCallCached(connection, "{call FIND_DEMO_NAME(?)}", 2);
        }
        try (resultSet) {
            assertTrue(resultSet.next());
            assertEquals("two", resultSet.getString("name"));
            assertFalse(resultSet.next());
        }
    }

    @Test
    void executeNamedCallBindsInParameters() throws Exception {
        try (Connection connection = openWithAliases()) {
            QueryUtils.executeNamedCall(connection, "{call INSERT_DEMO(:id, :name)}", Map.of("id", 12, "name", "twelve"));
            try (ResultSet resultSet = connection.createStatement().executeQuery(
                    "SELECT name FROM demo WHERE id = 12")) {
                assertTrue(resultSet.next());
                assertEquals("twelve", resultSet.getString("name"));
            }
        }
    }

    @Test
    void selectNamedFromCallReturnsRows() throws Exception {
        try (Connection connection = openWithAliases()) {
            try (ResultSet resultSet = QueryUtils.selectNamedFromCall(
                    connection,
                    NamedQuery.of("{call FIND_DEMO_NAME(:id)}"),
                    Map.of("id", 1))) {
                assertTrue(resultSet.next());
                assertEquals("one", resultSet.getString("name"));
                assertFalse(resultSet.next());
            }
        }
    }

    @Test
    void selectNamedFromCallCachedReturnsRowsAfterStatementClosed() throws Exception {
        ResultSet resultSet;
        try (Connection connection = openWithAliases()) {
            resultSet = QueryUtils.selectNamedFromCallCached(
                    connection,
                    "{call FIND_DEMO_NAME(:id)}",
                    Map.of("id", 2));
        }
        try (resultSet) {
            assertTrue(resultSet.next());
            assertEquals("two", resultSet.getString("name"));
            assertFalse(resultSet.next());
        }
    }

    private static Connection openWithAliases() throws Exception {
        Connection connection = H2Memory.open();
        H2Memory.bootstrapDemoSchema(connection);
        H2Memory.createAliases(connection);
        return connection;
    }
}
