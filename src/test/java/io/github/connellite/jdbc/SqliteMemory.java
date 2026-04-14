package io.github.connellite.jdbc;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * In-memory SQLite connections for tests ({@code jdbc:sqlite::memory:}).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SqliteMemory {

    /** Opens a fresh in-memory SQLite connection. */
    public static Connection open() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite::memory:");
    }

    /** Creates {@code demo(id, name)} with two rows and optional FK child table for key metadata. */
    public static void bootstrapDemoSchema(Connection c) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.execute("CREATE TABLE demo (id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL)");
            s.execute("INSERT INTO demo (id, name) VALUES (1, 'one')");
            s.execute("INSERT INTO demo (id, name) VALUES (2, 'two')");
            s.execute("CREATE TABLE child (cid INTEGER PRIMARY KEY, demo_id INTEGER NOT NULL REFERENCES demo(id), tag TEXT)");
            s.execute("INSERT INTO child (cid, demo_id, tag) VALUES (1, 1, 'c')");
        }
    }
}
