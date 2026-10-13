package io.github.connellite.jdbc.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface PreparedStatementCreator {
    PreparedStatement create(Connection connection, String sql) throws SQLException;
}
