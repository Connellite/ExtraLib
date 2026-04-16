package io.github.connellite.jdbc;

import lombok.experimental.Delegate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

class ResultSetWrapper implements ResultSet {
    @Delegate(types = ResultSet.class)
    private final ResultSet delegate;
    private final Statement statement;
    ResultSetWrapper(Statement statement, ResultSet delegate) {
        this.statement = statement;
        this.delegate = delegate;
    }
    @Override
    public void close() throws SQLException {
        SQLException closeException = null;
        try {
            delegate.close();
        } catch (SQLException e) {
            closeException = e;
        }
        try {
            statement.close();
        } catch (SQLException e) {
            if (closeException == null) {
                closeException = e;
            } else {
                closeException.addSuppressed(e);
            }
        }
        if (closeException != null) {
            throw closeException;
        }
    }
}