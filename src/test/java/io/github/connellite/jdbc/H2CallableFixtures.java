package io.github.connellite.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Java routines registered with H2 {@code CREATE ALIAS} from tests.
 */
public final class H2CallableFixtures {

    private H2CallableFixtures() {
    }

    public static int addValues(int left, int right) {
        return left + right;
    }

    public static void insertDemo(Connection connection, int id, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO demo (id, name) VALUES (?, ?)")) {
            statement.setInt(1, id);
            statement.setString(2, name);
            statement.executeUpdate();
        }
    }

    /**
     * The statement stays open so the caller can read the returned result set.
     */
    public static ResultSet findDemoName(Connection connection, int id) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT name FROM demo WHERE id = ?");
        statement.setInt(1, id);
        return statement.executeQuery();
    }
}
