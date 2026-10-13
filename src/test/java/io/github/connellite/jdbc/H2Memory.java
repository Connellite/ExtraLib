package io.github.connellite.jdbc;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * In-memory H2 connections for tests ({@code jdbc:h2:mem:}).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class H2Memory {

    /** Opens a private in-memory H2 connection. */
    public static Connection open() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:");
    }

    /**
     * Opens a private in-memory H2 connection in PostgreSQL compatibility mode,
     * so casts such as {@code ::int} are accepted.
     */
    public static Connection openPostgreSql() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:;MODE=PostgreSQL");
    }

    /** Opens a private in-memory H2 connection in MySQL compatibility mode, so {@code :=} is accepted. */
    public static Connection openMySql() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:;MODE=MySQL");
    }

    /**
     * Opens a private in-memory H2 connection in SQL Server compatibility mode.
     * Unquoted identifiers may start with {@code #}, and names keep their case.
     */
    public static Connection openMssql() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:;MODE=MSSQLServer;DATABASE_TO_UPPER=FALSE");
    }

    /** Creates {@code demo(id, name)} with two rows. */
    public static void bootstrapDemoSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE demo (id INTEGER NOT NULL PRIMARY KEY, name VARCHAR(50) NOT NULL)");
            statement.execute("INSERT INTO demo (id, name) VALUES (1, 'one')");
            statement.execute("INSERT INTO demo (id, name) VALUES (2, 'two')");
        }
    }

    /** Registers Java aliases used by callable tests. */
    public static void createAliases(Connection connection) throws SQLException {
        String type = H2CallableFixtures.class.getName();
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE ALIAS ADD_VALUES FOR \"" + type + ".addValues\"");
            statement.execute("CREATE ALIAS INSERT_DEMO FOR \"" + type + ".insertDemo\"");
            statement.execute("CREATE ALIAS FIND_DEMO_NAME FOR \"" + type + ".findDemoName\"");
        }
    }
}
