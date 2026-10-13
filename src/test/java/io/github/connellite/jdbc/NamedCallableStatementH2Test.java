package io.github.connellite.jdbc;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NamedCallableStatementH2Test {

    @Test
    void callBracesStayAndNamesBecomePositional() throws Exception {
        try (Connection connection = openWithAliases()) {
            try (NamedCallableStatement call = new NamedCallableStatement(
                    connection,
                    "{call INSERT_DEMO(:id, :name)}")) {
                assertEquals("{call INSERT_DEMO(?, ?)}", call.getParsedSql());
                assertEquals(List.of("id", "name"), call.getParameterOrder());
                assertFalse(connection.getMetaData().supportsNamedParameters());
                call.setInt("id", 10).setString("name", "ten");
                call.execute();
            }
            try (ResultSet resultSet = connection.createStatement().executeQuery(
                    "SELECT name FROM demo WHERE id = 10")) {
                assertTrue(resultSet.next());
                assertEquals("ten", resultSet.getString("name"));
            }
        }
    }

    @Test
    void namedQueryPrepareCallReadsFunctionReturn() throws Exception {
        try (Connection connection = openWithAliases()) {
            try (NamedCallableStatement call = NamedQuery.of("{:ret = call ADD_VALUES(:a, :b)}")
                    .setInt("a", 2)
                    .setInt("b", 3)
                    .registerOutParameter("ret", Types.INTEGER)
                    .prepareCall(connection)) {
                assertEquals("{? = call ADD_VALUES(?, ?)}", call.getParsedSql());
                call.execute();
                assertEquals(5, call.getInt("ret"));
            }
        }
    }

    @Test
    void functionReturnIsNamedAndReadByName() throws Exception {
        try (Connection connection = openWithAliases()) {
            try (NamedCallableStatement call = new NamedCallableStatement(
                    connection,
                    "{:ret = call ADD_VALUES(:a, :b)}")) {
                assertEquals("{? = call ADD_VALUES(?, ?)}", call.getParsedSql());
                assertEquals(List.of("ret", "a", "b"), call.getParameterOrder());
                call.setInt("a", 2).setInt("b", 3);
                call.registerOutParameter("ret", Types.INTEGER);
                call.execute();
                assertEquals(5, call.getInt("ret"));
                assertFalse(call.wasNull());
            }
        }
    }

    @Test
    void assignmentColonEqualsIsNotANamedParameter() throws Exception {
        try (Connection connection = H2Memory.openMySql()) {
            try (NamedCallableStatement call = new NamedCallableStatement(connection, "SELECT @v := :v")) {
                assertEquals("SELECT @v := ?", call.getParsedSql());
                assertEquals(List.of("v"), call.getParameterOrder());
                call.setInt("v", 4);
                try (ResultSet resultSet = call.executeQuery()) {
                    assertTrue(resultSet.next());
                    assertEquals(4, resultSet.getInt(1));
                }
            }
        }
    }

    @Test
    void executeQueryReturnsFirstResultSet() throws Exception {
        try (Connection connection = openWithAliases()) {
            try (NamedCallableStatement call = new NamedCallableStatement(
                    connection,
                    "{call FIND_DEMO_NAME(:id)}")) {
                call.setInt("id", 1);
                try (ResultSet resultSet = call.executeQuery()) {
                    assertTrue(resultSet.next());
                    assertEquals("one", resultSet.getString("name"));
                    assertFalse(resultSet.next());
                }
            }
        }
    }

    @Test
    void missingInOrOutParameterIsRejected() throws Exception {
        try (Connection connection = openWithAliases()) {
            try (NamedCallableStatement call = new NamedCallableStatement(
                    connection,
                    "{:ret = call ADD_VALUES(:a, :b)}")) {
                call.setInt("a", 1);
                call.registerOutParameter("ret", Types.INTEGER);
                IllegalStateException missing = assertThrows(IllegalStateException.class, call::execute);
                assertTrue(missing.getMessage().contains("b"));
            }
        }
    }

    @Test
    void mixingNamedAndPositionalThrowsBeforeExecute() throws Exception {
        try (Connection connection = H2Memory.open()) {
            assertThrows(IllegalArgumentException.class, () ->
                    new NamedCallableStatement(connection, "{? = call ADD_VALUES(:a, :b)}"));
        }
    }

    @Test
    void getRequiresASingleOccurrence() throws Exception {
        try (Connection connection = openWithAliases()) {
            try (NamedCallableStatement call = new NamedCallableStatement(
                    connection,
                    "{call ADD_VALUES(:n, :n)}")) {
                call.setInt("n", 1);
                call.execute();
                assertThrows(IllegalArgumentException.class, () -> call.getInt("n"));
            }
        }
    }

    @Test
    void batchRunsInParametersAndRejectsOutRegistration() throws Exception {
        try (Connection connection = openWithAliases()) {
            try (NamedCallableStatement call = new NamedCallableStatement(
                    connection,
                    "{call INSERT_DEMO(:id, :name)}")) {
                call.setInt("id", 10).setString("name", "ten");
                call.addBatch();
                call.setInt("id", 11).setString("name", "eleven");
                call.addBatch();
                assertEquals(2, call.executeBatch().length);
            }
            try (NamedCallableStatement call = new NamedCallableStatement(
                    connection,
                    "{:ret = call ADD_VALUES(:a, :b)}")) {
                call.setInt("a", 1).setInt("b", 2);
                call.registerOutParameter("ret", Types.INTEGER);
                assertThrows(IllegalStateException.class, call::addBatch);
            }
            try (ResultSet resultSet = connection.createStatement().executeQuery(
                    "SELECT COUNT(*) AS n FROM demo WHERE id IN (10, 11)")) {
                assertTrue(resultSet.next());
                assertEquals(2, resultSet.getInt("n"));
            }
        }
    }

    @Test
    void prepareCallHonorsResultSetTypeConcurrencyAndHoldability() throws Exception {
        try (Connection connection = openWithAliases()) {
            try (NamedCallableStatement call = new NamedCallableStatement(
                    connection,
                    "{:ret = call ADD_VALUES(:a, :b)}",
                    ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY,
                    ResultSet.CLOSE_CURSORS_AT_COMMIT)) {
                assertEquals("{? = call ADD_VALUES(?, ?)}", call.getParsedSql());
                call.setInt("a", 4).setInt("b", 6);
                call.registerOutParameter("ret", Types.INTEGER);
                call.execute();
                assertEquals(10, call.getInt("ret"));
            }
        }
    }

    private static Connection openWithAliases() throws Exception {
        Connection connection = H2Memory.open();
        H2Memory.bootstrapDemoSchema(connection);
        H2Memory.createAliases(connection);
        return connection;
    }
}
